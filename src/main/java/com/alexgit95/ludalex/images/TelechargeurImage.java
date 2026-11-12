package com.alexgit95.ludalex.images;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * Télécharge une image distante une seule fois (calcul de la couleur de fond).
 * La connexion ne peut viser que des adresses validées : le résolveur DNS applique la protection SSRF
 * au moment exact de la connexion, ce qui écarte le DNS rebinding.
 */
@Component
public class TelechargeurImage {

    private record Reponse(int code, String location, byte[] corps) {
    }

    private final UrlSortanteValidateur validateur;
    private final long tailleMax;
    private final Duration delai;
    private final int redirectionsMax;
    private final CloseableHttpClient client;

    public TelechargeurImage(ImagesProperties proprietes, UrlSortanteValidateur validateur) {
        this.validateur = validateur;
        this.tailleMax = proprietes.tailleMax().toBytes();
        this.delai = proprietes.delaiTelechargement();
        this.redirectionsMax = proprietes.redirectionsMax();

        Timeout timeout = Timeout.ofMilliseconds(delai.toMillis());
        DnsResolver resolveur = new DnsResolver() {
            @Override
            public java.net.InetAddress[] resolve(String hote) throws UnknownHostException {
                return validateur.adressesAutorisees(hote);
            }

            @Override
            public String resolveCanonicalHostname(String hote) {
                return hote;
            }
        };
        this.client = HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setDnsResolver(resolveur)
                        .setDefaultConnectionConfig(ConnectionConfig.custom()
                                .setConnectTimeout(timeout).setSocketTimeout(timeout).build())
                        .build())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setRedirectsEnabled(false).setResponseTimeout(timeout).build())
                .disableAutomaticRetries()
                .disableCookieManagement()
                .setUserAgent("ludalex")
                .build();
    }

    /** Renvoie les octets de l'image, bornés à la taille maximale ; lève une exception si l'URL est refusée ou injoignable. */
    public byte[] telecharger(String url) {
        long echeance = System.nanoTime() + delai.toNanos() * 2;
        URI courant = validateur.valider(url);
        for (int saut = 0; saut <= redirectionsMax; saut++) {
            Reponse reponse = appeler(courant, echeance);
            if (reponse.code() >= 300 && reponse.code() < 400 && reponse.location() != null) {
                courant = validateur.valider(courant.resolve(reponse.location()).toString());
                continue;
            }
            if (reponse.code() != 200) {
                throw new ImageInvalideException("Image inaccessible");
            }
            return reponse.corps();
        }
        throw new ImageInvalideException("Trop de redirections");
    }

    private Reponse appeler(URI uri, long echeance) {
        try {
            return client.execute(new HttpGet(uri), reponse -> lire(reponse, echeance));
        } catch (IOException e) {
            throw new ImageInvalideException("Image inaccessible", e);
        }
    }

    private Reponse lire(ClassicHttpResponse reponse, long echeance) throws IOException {
        int code = reponse.getCode();
        if (code >= 300 && code < 400) {
            Header location = reponse.getFirstHeader("Location");
            return new Reponse(code, location == null ? null : location.getValue(), null);
        }
        HttpEntity entite = reponse.getEntity();
        if (code != 200 || entite == null) {
            return new Reponse(code, null, null);
        }
        if (entite.getContentLength() > tailleMax) {
            throw new ImageInvalideException("Image trop volumineuse");
        }
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        byte[] tampon = new byte[8192];
        try (InputStream flux = entite.getContent()) {
            int lu;
            while ((lu = flux.read(tampon)) != -1) {
                if (sortie.size() + lu > tailleMax) {
                    throw new ImageInvalideException("Image trop volumineuse");
                }
                if (System.nanoTime() > echeance) {
                    throw new ImageInvalideException("Image inaccessible");
                }
                sortie.write(tampon, 0, lu);
            }
        }
        return new Reponse(code, null, sortie.toByteArray());
    }

    @PreDestroy
    void fermer() throws IOException {
        client.close();
    }
}

package com.alexgit95.ludalex.images;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Protection SSRF : toute requête sortante vers une URL saisie passe par ce contrôle,
 * à la validation de l'URL puis à chaque résolution DNS effective de la connexion.
 */
@Component
public class UrlSortanteValidateur {

    static final int LONGUEUR_MAX = 2048;

    /** Résolution DNS remplaçable dans les tests. */
    @FunctionalInterface
    interface Resolveur {
        InetAddress[] resoudre(String hote) throws UnknownHostException;
    }

    private final Resolveur resolveur;
    private final Predicate<InetAddress> interdite;

    @Autowired
    public UrlSortanteValidateur() {
        this(InetAddress::getAllByName, UrlSortanteValidateur::estInterdite);
    }

    UrlSortanteValidateur(Resolveur resolveur, Predicate<InetAddress> interdite) {
        this.resolveur = resolveur;
        this.interdite = interdite;
    }

    /** Contrôle de saisie : URL http(s) bien formée, sans identifiants. Aucune requête n'est émise. */
    public URI validerSyntaxe(String url) {
        if (url == null || url.isBlank() || url.length() > LONGUEUR_MAX) {
            throw new ImageInvalideException("Adresse d'image invalide");
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new ImageInvalideException("Adresse d'image invalide", e);
        }
        String schema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!schema.equals("http") && !schema.equals("https")) {
            throw new ImageInvalideException("Seules les adresses http et https sont acceptées");
        }
        if (uri.getHost() == null || uri.getUserInfo() != null) {
            throw new ImageInvalideException("Adresse d'image invalide");
        }
        return uri;
    }

    /** Contrôle avant requête sortante : syntaxe puis adresses résolues. */
    public URI valider(String url) {
        URI uri = validerSyntaxe(url);
        try {
            adressesAutorisees(uri.getHost());
        } catch (UnknownHostException e) {
            throw new ImageInvalideException("Adresse d'image non autorisée", e);
        }
        return uri;
    }

    /** Résout l'hôte et refuse dès qu'une adresse est interne ; sert aussi de résolveur DNS au client HTTP. */
    public InetAddress[] adressesAutorisees(String hote) throws UnknownHostException {
        InetAddress[] adresses = resolveur.resoudre(hote);
        if (adresses.length == 0) {
            throw new UnknownHostException("Hôte inconnu");
        }
        for (InetAddress adresse : adresses) {
            if (interdite.test(adresse)) {
                throw new UnknownHostException("Adresse non autorisée");
            }
        }
        return adresses;
    }

    static boolean estInterdite(InetAddress adresse) {
        if (adresse.isAnyLocalAddress() || adresse.isLoopbackAddress() || adresse.isLinkLocalAddress()
                || adresse.isSiteLocalAddress() || adresse.isMulticastAddress()) {
            return true;
        }
        byte[] b = adresse.getAddress();
        if (b.length == 4) {
            int premier = b[0] & 0xFF;
            int second = b[1] & 0xFF;
            return premier == 0
                    || (premier == 100 && second >= 64 && second <= 127)
                    || (premier == 192 && second == 0 && b[2] == 0)
                    || (premier == 198 && (second == 18 || second == 19))
                    || premier >= 240;
        }
        // ULA fc00::/7, NAT64 64:ff9b::/96, 6to4 2002::/16
        return (b[0] & 0xFE) == 0xFC
                || (b[0] == 0x00 && b[1] == 0x64 && (b[2] & 0xFF) == 0xFF && (b[3] & 0xFF) == 0x9B)
                || (b[0] == 0x20 && b[1] == 0x02);
    }
}

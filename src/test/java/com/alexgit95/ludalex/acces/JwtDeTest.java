package com.alexgit95.ludalex.acces;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Émet des JWT signés localement, à la manière de Cloudflare Access. */
final class JwtDeTest {

    static final String EMETTEUR = "https://equipe.cloudflareaccess.com";
    static final String AUDIENCE = "audience-ludalex";

    private final RSAKey cle;

    JwtDeTest() {
        try {
            cle = new RSAKeyGenerator(2048).keyID("cle-test").generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    JwtDecoder decodeur() {
        NimbusJwtDecoder decodeur = NimbusJwtDecoder.withPublicKey(publique()).build();
        decodeur.setJwtValidator(CloudflareJwtValidateurs.creer(EMETTEUR, AUDIENCE));
        return decodeur;
    }

    String jetonValide() {
        return jeton(EMETTEUR, AUDIENCE, Instant.now().plusSeconds(600));
    }

    String jeton(String emetteur, String audience, Instant expiration) {
        try {
            JWTClaimsSet revendications = new JWTClaimsSet.Builder()
                    .issuer(emetteur)
                    .audience(audience)
                    .subject("sujet-anonyme")
                    .issueTime(Date.from(Instant.now().minusSeconds(900)))
                    .expirationTime(Date.from(expiration))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(cle.getKeyID()).build(), revendications);
            jwt.sign(new RSASSASigner(cle));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private RSAPublicKey publique() {
        try {
            return cle.toRSAPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

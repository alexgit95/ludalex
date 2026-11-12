package com.alexgit95.ludalex.parties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Génération, vérification et consommation des tokens de fin de partie. */
@Service
@Transactional
public class TokenFinService {

    private static final int OCTETS = 32;
    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private final TokenFinPartieRepository tokens;
    private final FinPartieProperties proprietes;
    private final Clock horloge;
    private final SecureRandom aleatoire = new SecureRandom();

    public TokenFinService(TokenFinPartieRepository tokens, FinPartieProperties proprietes, Clock horloge) {
        this.tokens = tokens;
        this.proprietes = proprietes;
        this.horloge = horloge;
    }

    /** Crée un token (256 bits) et invalide les précédents ; le clair n'est renvoyé qu'ici. */
    public String generer(Partie partie) {
        tokens.invaliderPourPartie(partie.getId());
        byte[] octets = new byte[OCTETS];
        aleatoire.nextBytes(octets);
        String clair = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        tokens.save(new TokenFinPartie(partie, empreinte(clair), Instant.now(horloge).plus(proprietes.validite())));
        return clair;
    }

    public void invaliderPour(Long partieId) {
        tokens.invaliderPourPartie(partieId);
    }

    /** Token utilisable : bien formé, connu, non expiré, non utilisé, partie en cours. Ne consomme rien. */
    @Transactional(readOnly = true)
    public Optional<TokenFinPartie> verifier(String clair) {
        if (clair == null || !FORMAT.matcher(clair).matches()) {
            return Optional.empty();
        }
        return tokens.trouverParEmpreinte(empreinte(clair))
                .filter(token -> estUtilisable(token, Instant.now(horloge)));
    }

    /**
     * Consomme le token de façon atomique ; à appeler en tête de la transaction de l'action autorisée,
     * pour que tout échec ultérieur annule aussi la consommation.
     */
    public TokenFinPartie consommer(String clair) {
        TokenFinPartie token = verifier(clair).orElseThrow(TokenFinInvalideException::new);
        if (tokens.consommer(token.getId(), Instant.now(horloge)) != 1) {
            throw new TokenFinInvalideException();
        }
        return token;
    }

    @Transactional(readOnly = true)
    public boolean aUnTokenValide(Long partieId) {
        return !tokens.valides(partieId, Instant.now(horloge)).isEmpty();
    }

    static String empreinte(String clair) {
        try {
            byte[] hachage = MessageDigest.getInstance("SHA-256").digest(clair.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hachage);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean estUtilisable(TokenFinPartie token, Instant maintenant) {
        return !token.isUtilise() && token.getExpireLe().isAfter(maintenant)
                && token.getPartie().getStatut() == StatutPartie.EN_COURS;
    }
}

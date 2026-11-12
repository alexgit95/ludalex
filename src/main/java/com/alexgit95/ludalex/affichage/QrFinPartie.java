package com.alexgit95.ludalex.affichage;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.TokenFinService;

/**
 * Détient le token de fin de partie en clair le temps de l'afficher dans le QR code : la base n'en garde que
 * l'empreinte. Seule la mémoire du serveur le conserve, et il disparaît au redémarrage.
 */
@Component
public class QrFinPartie {

    private record Jeton(Long partieId, String clair) {
    }

    private final TokenFinService tokens;
    private final AffichageProperties proprietes;
    private final Clock horloge;
    private Jeton courant;

    public QrFinPartie(TokenFinService tokens, AffichageProperties proprietes, Clock horloge) {
        this.tokens = tokens;
        this.proprietes = proprietes;
        this.horloge = horloge;
    }

    /** Réutilise le token tant qu'il vaut encore plus que le délai de renouvellement, sinon en génère un. */
    public synchronized String obtenir(Partie partie) {
        if (courant != null && courant.partieId().equals(partie.getId()) && encoreAssezValide(courant.clair())) {
            return courant.clair();
        }
        return regenerer(partie);
    }

    /** Nouveau token : l'ancien non utilisé est invalidé. */
    public synchronized String regenerer(Partie partie) {
        String clair = tokens.generer(partie);
        courant = new Jeton(partie.getId(), clair);
        return clair;
    }

    synchronized Optional<String> courant(Long partieId) {
        return courant != null && courant.partieId().equals(partieId) ? Optional.of(courant.clair()) : Optional.empty();
    }

    private boolean encoreAssezValide(String clair) {
        Instant limite = Instant.now(horloge).plus(proprietes.renouvellementToken());
        return tokens.verifier(clair).filter(token -> token.getExpireLe().isAfter(limite)).isPresent();
    }
}

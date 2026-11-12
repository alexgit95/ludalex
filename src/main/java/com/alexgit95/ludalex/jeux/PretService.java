package com.alexgit95.ludalex.jeux;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PretService {

    private final JeuRepository jeux;
    private final PretRepository prets;
    private final Clock horloge;

    public PretService(JeuRepository jeux, PretRepository prets, Clock horloge) {
        this.jeux = jeux;
        this.prets = prets;
        this.horloge = horloge;
    }

    /** Un seul prêt en cours par jeu : le verrou sur le jeu sérialise les demandes simultanées. */
    public Pret preter(Long jeuId, String emprunteur, LocalDate datePret) {
        Jeu jeu = jeux.trouverPourMiseAJour(jeuId).orElseThrow(() -> new JeuIntrouvableException(jeuId));
        String nom = emprunteur == null ? "" : emprunteur.trim();
        if (nom.isEmpty()) {
            throw new PretInvalideException("Le nom de l'emprunteur est obligatoire");
        }
        if (nom.length() > Pret.EMPRUNTEUR_MAX) {
            throw new PretInvalideException(
                    "Le nom de l'emprunteur ne doit pas dépasser " + Pret.EMPRUNTEUR_MAX + " caractères");
        }
        if (prets.trouverEnCours(jeuId).isPresent()) {
            throw new PretInvalideException("Ce jeu est déjà prêté");
        }
        return prets.save(new Pret(jeu, nom, datePret != null ? datePret : LocalDate.now(horloge)));
    }

    public Pret retourner(Long jeuId, LocalDate dateRetour) {
        jeux.trouverPourMiseAJour(jeuId).orElseThrow(() -> new JeuIntrouvableException(jeuId));
        Pret pret = prets.trouverEnCours(jeuId)
                .orElseThrow(() -> new PretInvalideException("Ce jeu n'est pas prêté"));
        LocalDate retour = dateRetour != null ? dateRetour : LocalDate.now(horloge);
        if (retour.isBefore(pret.getDatePret())) {
            throw new PretInvalideException("La date de retour ne peut pas précéder la date de prêt");
        }
        pret.clore(retour);
        return prets.save(pret);
    }
}

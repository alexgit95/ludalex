package com.alexgit95.ludalex.affichage;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.DetailPartie;
import com.alexgit95.ludalex.parties.Participation;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.PartieEnCoursService;
import com.alexgit95.ludalex.parties.PartieRepository;
import com.alexgit95.ludalex.parties.PartieService;
import com.alexgit95.ludalex.parties.ResultatCooperatif;
import com.alexgit95.ludalex.parties.StatutPartie;
import com.alexgit95.ludalex.parties.VuePartie;

/** Détermine ce que montre l'écran : partie en cours, pause, victoire récente ou veille. */
@Service
@Transactional
public class AffichageService {

    private final PartieEnCoursService enCours;
    private final PartieService parties;
    private final PartieRepository repertoire;
    private final QrFinPartie qr;
    private final CodeQr codeQr;
    private final DiaporamaImages diaporama;
    private final AffichageProperties proprietes;
    private final Clock horloge;

    public AffichageService(PartieEnCoursService enCours, PartieService parties, PartieRepository repertoire,
            QrFinPartie qr, CodeQr codeQr, DiaporamaImages diaporama, AffichageProperties proprietes, Clock horloge) {
        this.enCours = enCours;
        this.parties = parties;
        this.repertoire = repertoire;
        this.qr = qr;
        this.codeQr = codeQr;
        this.diaporama = diaporama;
        this.proprietes = proprietes;
        this.horloge = horloge;
    }

    /**
     * @param origine      schéma et domaine publics, pour l'adresse encodée dans le QR code
     * @param nouveauToken vrai à l'ouverture de la page : un nouveau token est généré, l'ancien invalidé
     */
    public EtatAffichage etat(String origine, boolean nouveauToken) {
        Optional<VuePartie> active = enCours.active();
        if (active.isPresent()) {
            return etatPartieActive(active.get(), origine, nouveauToken);
        }
        Instant depuis = Instant.now(horloge).minus(proprietes.dureeVictoire());
        List<Partie> recentes = repertoire.terminesDepuis(depuis, PageRequest.of(0, 1));
        if (!recentes.isEmpty()) {
            return etatVictoire(recentes.get(0));
        }
        return new EtatAffichage(EtatAffichage.Type.VEILLE, null, null, List.of(), false, diaporama.urls(), "VEILLE");
    }

    private EtatAffichage etatPartieActive(VuePartie vue, String origine, boolean nouveauToken) {
        Partie partie = vue.partie();
        if (partie.getStatut() == StatutPartie.EN_PAUSE) {
            Long suivant = vue.joueurSuivant() == null ? null : vue.joueurSuivant().getId();
            return new EtatAffichage(EtatAffichage.Type.EN_PAUSE, vue, null, List.of(), false, List.of(),
                    "EN_PAUSE:" + partie.getId() + ":" + suivant);
        }
        String clair = nouveauToken ? qr.regenerer(partie) : qr.obtenir(partie);
        String svg = codeQr.svg(origine + "/fin/" + clair);
        return new EtatAffichage(EtatAffichage.Type.EN_COURS, vue, svg, List.of(), false, List.of(),
                "EN_COURS:" + partie.getId() + ":" + Integer.toHexString(clair.hashCode()));
    }

    private EtatAffichage etatVictoire(Partie terminee) {
        DetailPartie detail = parties.detail(terminee.getId());
        VuePartie vue = new VuePartie(detail.partie(), detail.partie().getJeu(), detail.participations(),
                detail.equipes(), null);
        List<Joueur> vainqueurs = detail.participations().stream().filter(Participation::isGagnant)
                .map(Participation::getJoueur).toList();
        boolean defaite = terminee.getResultatCooperatif() == ResultatCooperatif.DEFAITE;
        return new EtatAffichage(EtatAffichage.Type.VICTOIRE, vue, null, vainqueurs, defaite, List.of(),
                "VICTOIRE:" + terminee.getId() + ":" + terminee.getTermineeLe().toEpochMilli());
    }
}

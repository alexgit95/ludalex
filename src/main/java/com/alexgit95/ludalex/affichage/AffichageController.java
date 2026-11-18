package com.alexgit95.ludalex.affichage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Écran Affichage : page complète, puis fragment mis à jour par interrogation légère. */
@Controller
@RequestMapping("/affichage")
public class AffichageController {

    private final AffichageService affichage;
    private final OriginePublique origine;
    private final HistoriqueAffichage historique;
        private final CodeQr codeQr;

        public AffichageController(AffichageService affichage, OriginePublique origine, HistoriqueAffichage historique,
            CodeQr codeQr) {
        this.affichage = affichage;
        this.origine = origine;
        this.historique = historique;
        this.codeQr = codeQr;
    }

    /** Ouverture ou rechargement : un nouveau token est généré pour le QR code. */
    @GetMapping
    public String page(HttpServletRequest requete, HttpServletResponse reponse, Model model) {
        reponse.setHeader("Cache-Control", "no-store");
        String adresse = origine.depuis(requete);
        ajouterEtat(model, affichage.etat(adresse, true), adresse);
        return "affichage/page";
    }

    /** Répond 204 (rien à changer) tant que l'état connu de l'écran est à jour. */
    @GetMapping("/etat")
    public String etat(@RequestParam(required = false) String signature, HttpServletRequest requete,
            HttpServletResponse reponse, Model model) {
        reponse.setHeader("Cache-Control", "no-store");
        String adresse = origine.depuis(requete);
        EtatAffichage etat = affichage.etat(adresse, false);
        if (etat.signature().equals(signature)) {
            reponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return "affichage/vide";
        }
        ajouterEtat(model, etat, adresse);
        return "affichage/etat :: etat";
    }

    private void ajouterEtat(Model model, EtatAffichage etat, String adresse) {
        model.addAttribute("etat", etat);
        if (etat.type() == EtatAffichage.Type.EN_COURS) {
            model.addAttribute("historique", historique.pourJeu(etat.vue().jeu().getId()));
        } else if (etat.type() == EtatAffichage.Type.VEILLE) {
            model.addAttribute("historique", historique.global());
            model.addAttribute("qrLancement", codeQr.svgLancement(adresse + "/parties/direct"));
        }
    }
}

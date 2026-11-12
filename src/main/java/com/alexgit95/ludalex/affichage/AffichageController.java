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

    public AffichageController(AffichageService affichage, OriginePublique origine) {
        this.affichage = affichage;
        this.origine = origine;
    }

    /** Ouverture ou rechargement : un nouveau token est généré pour le QR code. */
    @GetMapping
    public String page(HttpServletRequest requete, HttpServletResponse reponse, Model model) {
        reponse.setHeader("Cache-Control", "no-store");
        model.addAttribute("etat", affichage.etat(origine.depuis(requete), true));
        return "affichage/page";
    }

    /** Répond 204 (rien à changer) tant que l'état connu de l'écran est à jour. */
    @GetMapping("/etat")
    public String etat(@RequestParam(required = false) String signature, HttpServletRequest requete,
            HttpServletResponse reponse, Model model) {
        reponse.setHeader("Cache-Control", "no-store");
        EtatAffichage etat = affichage.etat(origine.depuis(requete), false);
        if (etat.signature().equals(signature)) {
            reponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return "affichage/vide";
        }
        model.addAttribute("etat", etat);
        return "affichage/etat :: etat";
    }
}

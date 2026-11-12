package com.alexgit95.ludalex.parties;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Page publique de fin de partie, ouverte par le QR code sans authentification Cloudflare :
 * le token est la seule autorisation et n'expose que sa partie.
 */
@Controller
@RequestMapping("/fin/{token}")
public class FinPartieController {

    private static final String FORMULAIRE = "fin/formulaire";
    private static final String INCONNU = "fin/inconnu";

    private final PartieEnCoursService enCours;

    public FinPartieController(PartieEnCoursService enCours) {
        this.enCours = enCours;
    }

    @GetMapping
    public String afficher(@PathVariable String token, Model model, HttpServletResponse reponse) {
        return enCours.vueDuLien(token)
                .map(vue -> formulaire(model, token, vue, new FinForm(), null))
                .orElseGet(() -> neutre(reponse));
    }

    @PostMapping("/terminer")
    public String terminer(@PathVariable String token, @ModelAttribute("form") FinForm form, BindingResult erreurs,
            Model model, HttpServletResponse reponse) {
        String erreur = erreurs.hasErrors() ? "Une valeur saisie est invalide" : null;
        if (erreur == null) {
            try {
                enCours.terminerViaToken(token, form.versSaisie());
                return "fin/termine";
            } catch (TokenFinInvalideException e) {
                return neutre(reponse);
            } catch (PartieInvalideException e) {
                erreur = e.getMessage();
            }
        }
        return reafficher(token, form, erreur, model, reponse);
    }

    @PostMapping("/pause")
    public String pauser(@PathVariable String token, @RequestParam(required = false) Long joueurSuivantId,
            Model model, HttpServletResponse reponse) {
        try {
            enCours.pauserViaToken(token, joueurSuivantId);
            return "fin/pause";
        } catch (TokenFinInvalideException e) {
            return neutre(reponse);
        } catch (PartieInvalideException e) {
            return reafficher(token, new FinForm(), e.getMessage(), model, reponse);
        }
    }

    private String reafficher(String token, FinForm form, String erreur, Model model, HttpServletResponse reponse) {
        return enCours.vueDuLien(token)
                .map(vue -> formulaire(model, token, vue, form, erreur))
                .orElseGet(() -> neutre(reponse));
    }

    private String formulaire(Model model, String token, VuePartie vue, FinForm form, String erreur) {
        model.addAttribute("vue", vue);
        model.addAttribute("form", form);
        model.addAttribute("token", token);
        model.addAttribute("erreur", erreur);
        return FORMULAIRE;
    }

    /** Même réponse pour un lien inconnu, expiré, déjà utilisé ou dont la partie n'est plus en cours. */
    private String neutre(HttpServletResponse reponse) {
        reponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return INCONNU;
    }
}

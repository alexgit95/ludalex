package com.alexgit95.ludalex.parties;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alexgit95.ludalex.jeux.JeuService;
import com.alexgit95.ludalex.joueurs.JoueurService;

/** Écrans du propriétaire pour la partie en direct : lancement, pause, reprise, fin. */
@Controller
@RequestMapping("/parties/direct")
public class PartieEnCoursController {

    private static final String PAGE = "parties/direct";
    private static final String PAGE_FIN = "parties/direct-terminer";
    private static final String REDIRECTION = "redirect:/parties/direct";

    private final PartieEnCoursService enCours;
    private final JeuService jeux;
    private final JoueurService joueurs;

    public PartieEnCoursController(PartieEnCoursService enCours, JeuService jeux, JoueurService joueurs) {
        this.enCours = enCours;
        this.jeux = jeux;
        this.joueurs = joueurs;
    }

    @GetMapping
    public String page(Model model) {
        Optional<VuePartie> active = enCours.active();
        model.addAttribute("active", active.orElse(null));
        if (active.isEmpty()) {
            preparerLancement(model, new LancementForm());
        }
        return PAGE;
    }

    @PostMapping("/lancer")
    public String lancer(@ModelAttribute("form") LancementForm form, BindingResult erreurs, Model model,
            RedirectAttributes redirection) {
        if (!erreurs.hasErrors()) {
            try {
                enCours.lancer(form.versSaisie());
                redirection.addFlashAttribute("message", "Partie lancée");
                return REDIRECTION;
            } catch (PartieActiveException e) {
                return pageFinPrecedente(model, e.getPartieActiveId(), form, new FinForm(), null);
            } catch (PartieInvalideException e) {
                erreurs.reject("invalide", e.getMessage());
            }
        }
        model.addAttribute("active", null);
        preparerLancement(model, form);
        return PAGE;
    }

    @PostMapping("/{id}/terminer-et-lancer")
    public String terminerEtLancer(@PathVariable Long id, @ModelAttribute("fin") FinForm fin,
            BindingResult erreursFin, @ModelAttribute("form") LancementForm lancement,
            BindingResult erreursLancement, Model model, RedirectAttributes redirection) {
        String erreur = erreursFin.hasErrors() || erreursLancement.hasErrors() ? "Une valeur saisie est invalide" : null;
        if (erreur == null) {
            try {
                enCours.terminerEtLancer(id, fin.versSaisie(), lancement.versSaisie());
                redirection.addFlashAttribute("message", "Partie précédente terminée, nouvelle partie lancée");
                return REDIRECTION;
            } catch (PartieInvalideException e) {
                erreur = e.getMessage();
            } catch (PartieActiveException e) {
                erreur = e.getMessage();
            }
        }
        return pageFinPrecedente(model, id, lancement, fin, erreur);
    }

    @GetMapping("/{id}/terminer")
    public String pageFin(@PathVariable Long id, Model model, RedirectAttributes redirection) {
        try {
            remplirFin(model, enCours.vue(id), new FinForm(), null);
        } catch (PartieInvalideException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
            return REDIRECTION;
        }
        model.addAttribute("action", "/parties/direct/" + id + "/terminer");
        model.addAttribute("lancement", null);
        return PAGE_FIN;
    }

    @PostMapping("/{id}/terminer")
    public String terminer(@PathVariable Long id, @ModelAttribute("fin") FinForm fin, BindingResult erreurs,
            Model model, RedirectAttributes redirection) {
        String erreur = erreurs.hasErrors() ? "Une valeur saisie est invalide" : null;
        if (erreur == null) {
            try {
                enCours.terminer(id, fin.versSaisie());
                redirection.addFlashAttribute("message", "Partie terminée");
                return REDIRECTION;
            } catch (PartieInvalideException e) {
                erreur = e.getMessage();
            }
        }
        remplirFin(model, enCours.vue(id), fin, erreur);
        model.addAttribute("action", "/parties/direct/" + id + "/terminer");
        model.addAttribute("lancement", null);
        return PAGE_FIN;
    }

    @PostMapping("/{id}/pause")
    public String pauser(@PathVariable Long id, @RequestParam(required = false) Long joueurSuivantId,
            RedirectAttributes redirection) {
        try {
            enCours.pauser(id, joueurSuivantId);
            redirection.addFlashAttribute("message", "Partie mise en pause");
        } catch (PartieInvalideException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return REDIRECTION;
    }

    @PostMapping("/{id}/reprendre")
    public String reprendre(@PathVariable Long id, RedirectAttributes redirection) {
        try {
            enCours.reprendre(id);
            redirection.addFlashAttribute("message", "Partie reprise");
        } catch (PartieInvalideException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return REDIRECTION;
    }

    private String pageFinPrecedente(Model model, Long precedenteId, LancementForm lancement, FinForm fin,
            String erreur) {
        remplirFin(model, enCours.vue(precedenteId), fin, erreur);
        model.addAttribute("action", "/parties/direct/" + precedenteId + "/terminer-et-lancer");
        model.addAttribute("lancement", lancement);
        return PAGE_FIN;
    }

    private void remplirFin(Model model, VuePartie vue, FinForm fin, String erreur) {
        model.addAttribute("vue", vue);
        model.addAttribute("fin", fin);
        model.addAttribute("erreur", erreur);
    }

    private void preparerLancement(Model model, LancementForm form) {
        model.addAttribute("form", form);
        model.addAttribute("jeux", jeux.tousParNom());
        model.addAttribute("joueurs", joueurs.selectionnables());
        model.addAttribute("equipesMax", PartieService.EQUIPES_MAX);
    }
}

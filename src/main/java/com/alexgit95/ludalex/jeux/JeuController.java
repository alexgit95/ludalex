package com.alexgit95.ludalex.jeux;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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

@Controller
@RequestMapping("/jeux")
public class JeuController {

    private static final String FORMULAIRE = "jeux/formulaire";
    private static final String REDIRECTION_LISTE = "redirect:/jeux";

    private final JeuService jeux;
    private final PretService prets;
    private final Clock horloge;
    private final ChampionsDesJeux champions;

    public JeuController(JeuService jeux, PretService prets, Clock horloge, ChampionsDesJeux champions) {
        this.jeux = jeux;
        this.prets = prets;
        this.horloge = horloge;
        this.champions = champions;
    }

    @GetMapping
    public String liste(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<VignetteJeu> vignettes = jeux.lister(page);
        model.addAttribute("page", vignettes);
        model.addAttribute("champions",
                champions.pour(vignettes.getContent().stream().map(vignette -> vignette.jeu().getId()).toList()));
        return "jeux/liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        preparer(model, new JeuForm(), null);
        return FORMULAIRE;
    }

    @PostMapping
    public String creer(@ModelAttribute("form") JeuForm form, BindingResult erreurs, Model model,
            RedirectAttributes redirection) {
        if (!erreurs.hasErrors()) {
            try {
                Jeu jeu = jeux.creer(form.versSaisie());
                redirection.addFlashAttribute("message", "Jeu créé");
                return "redirect:/jeux/" + jeu.getId();
            } catch (JeuInvalideException e) {
                erreurs.rejectValue(champ(e), "invalide", e.getMessage());
            }
        }
        preparer(model, form, null);
        return FORMULAIRE;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("fiche", jeux.fiche(id));
        model.addAttribute("aujourdhui", LocalDate.now(horloge));
        return "jeux/detail";
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id, Model model) {
        Jeu jeu = jeux.trouver(id);
        preparer(model, JeuForm.depuis(jeu), jeu);
        return FORMULAIRE;
    }

    @PostMapping("/{id}")
    public String enregistrer(@PathVariable Long id, @ModelAttribute("form") JeuForm form, BindingResult erreurs,
            Model model, RedirectAttributes redirection) {
        if (!erreurs.hasErrors()) {
            try {
                jeux.modifier(id, form.versSaisie());
                redirection.addFlashAttribute("message", "Jeu modifié");
                return "redirect:/jeux/" + id;
            } catch (JeuInvalideException e) {
                erreurs.rejectValue(champ(e), "invalide", e.getMessage());
            }
        }
        preparer(model, form, jeux.trouver(id));
        return FORMULAIRE;
    }

    /** Fragment HTMX demandant la confirmation avant suppression. */
    @GetMapping("/{id}/suppression")
    public String confirmation(@PathVariable Long id, Model model) {
        model.addAttribute("jeu", jeux.trouver(id));
        return "jeux/confirmation :: confirmation";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, RedirectAttributes redirection) {
        try {
            jeux.supprimer(id);
        } catch (JeuInvalideException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/jeux/" + id;
        }
        redirection.addFlashAttribute("message", "Jeu supprimé");
        return REDIRECTION_LISTE;
    }

    @GetMapping("/{id}/pret")
    public String formulairePret(@PathVariable Long id, Model model) {
        model.addAttribute("jeu", jeux.trouver(id));
        model.addAttribute("aujourdhui", LocalDate.now(horloge));
        return "jeux/pret :: pret";
    }

    @PostMapping("/{id}/preter")
    public String preter(@PathVariable Long id, @RequestParam String emprunteur,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datePret,
            RedirectAttributes redirection) {
        try {
            prets.preter(id, emprunteur, datePret);
            redirection.addFlashAttribute("message", "Prêt enregistré");
        } catch (PretInvalideException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/jeux/" + id;
    }

    @GetMapping("/{id}/retour")
    public String formulaireRetour(@PathVariable Long id, Model model) {
        model.addAttribute("jeu", jeux.trouver(id));
        model.addAttribute("aujourdhui", LocalDate.now(horloge));
        return "jeux/pret :: retour";
    }

    @PostMapping("/{id}/retour")
    public String retourner(@PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateRetour,
            RedirectAttributes redirection) {
        try {
            prets.retourner(id, dateRetour);
            redirection.addFlashAttribute("message", "Retour enregistré");
        } catch (PretInvalideException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/jeux/" + id;
    }

    private void preparer(Model model, JeuForm form, Jeu jeu) {
        model.addAttribute("form", form);
        model.addAttribute("jeu", jeu);
        model.addAttribute("action", jeu == null ? "/jeux" : "/jeux/" + jeu.getId());
    }

    private static String champ(JeuInvalideException e) {
        return "image".equals(e.getChamp()) ? "imageUrl" : e.getChamp();
    }
}

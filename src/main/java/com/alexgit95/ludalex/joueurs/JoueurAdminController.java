package com.alexgit95.ludalex.joueurs;

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
@RequestMapping("/admin/joueurs")
public class JoueurAdminController {

    private static final String FORMULAIRE = "admin/joueurs/formulaire";
    private static final String REDIRECTION_LISTE = "redirect:/admin/joueurs";

    private final JoueurService service;

    public JoueurAdminController(JoueurService service) {
        this.service = service;
    }

    @GetMapping
    public String liste(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", service.lister(page));
        return "admin/joueurs/liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        preparer(model, new JoueurForm(), null);
        return FORMULAIRE;
    }

    @PostMapping
    public String creer(@ModelAttribute("form") JoueurForm form, BindingResult erreurs, Model model,
            RedirectAttributes redirection) {
        try {
            service.creer(form.getNom(), form.avatar(), form.victoire());
        } catch (JoueurInvalideException e) {
            erreurs.rejectValue(champ(e), "invalide", e.getMessage());
            preparer(model, form, null);
            return FORMULAIRE;
        }
        redirection.addFlashAttribute("message", "Joueur créé");
        return REDIRECTION_LISTE;
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id, Model model) {
        Joueur joueur = service.trouverActif(id);
        JoueurForm form = new JoueurForm();
        form.setNom(joueur.getNom());
        preparer(model, form, joueur);
        return FORMULAIRE;
    }

    @PostMapping("/{id}")
    public String enregistrer(@PathVariable Long id, @ModelAttribute("form") JoueurForm form, BindingResult erreurs,
            Model model, RedirectAttributes redirection) {
        try {
            service.modifier(id, form.getNom(), form.avatar(), form.victoire());
        } catch (JoueurInvalideException e) {
            erreurs.rejectValue(champ(e), "invalide", e.getMessage());
            preparer(model, form, service.trouverActif(id));
            return FORMULAIRE;
        }
        redirection.addFlashAttribute("message", "Joueur modifié");
        return REDIRECTION_LISTE;
    }

    /** Fragment HTMX demandant la confirmation avant suppression. */
    @GetMapping("/{id}/suppression")
    public String confirmation(@PathVariable Long id, Model model) {
        model.addAttribute("joueur", service.trouverActif(id));
        return "admin/joueurs/confirmation :: confirmation";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, RedirectAttributes redirection) {
        service.supprimer(id);
        redirection.addFlashAttribute("message", "Joueur supprimé");
        return REDIRECTION_LISTE;
    }

    private void preparer(Model model, JoueurForm form, Joueur joueur) {
        model.addAttribute("form", form);
        model.addAttribute("joueur", joueur);
        model.addAttribute("action", joueur == null ? "/admin/joueurs" : "/admin/joueurs/" + joueur.getId());
    }

    private static String champ(JoueurInvalideException e) {
        return "nom".equals(e.getChamp()) ? "nom" : e.getChamp() + "Url";
    }
}

package com.alexgit95.ludalex.reglages;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/reglages")
public class ReglagesAdminController {

    private static final String PAGE = "admin/reglages";

    /** Une ligne du formulaire ; {@code valeur} est la saisie à réafficher en cas d'erreur. */
    public record LigneReglage(String nom, String libelle, String valeur, int defaut, int min, int max, String erreur) {
    }

    private final ReglagesService service;

    public ReglagesAdminController(ReglagesService service) {
        this.service = service;
    }

    @GetMapping
    public String afficher(Model model) {
        Map<ReglageCle, Integer> valeurs = service.tous();
        Map<ReglageCle, String> saisies = new EnumMap<>(ReglageCle.class);
        valeurs.forEach((cle, valeur) -> saisies.put(cle, Integer.toString(valeur)));
        model.addAttribute("lignes", lignes(saisies, Map.of()));
        return PAGE;
    }

    @PostMapping
    public String enregistrer(@RequestParam Map<String, String> parametres, Model model,
            RedirectAttributes redirection) {
        Map<ReglageCle, String> saisies = new EnumMap<>(ReglageCle.class);
        for (ReglageCle cle : ReglageCle.values()) {
            saisies.put(cle, parametres.getOrDefault(cle.name(), ""));
        }
        try {
            service.enregistrer(saisies);
        } catch (ReglageInvalideException e) {
            model.addAttribute("lignes", lignes(saisies, e.erreurs()));
            return PAGE;
        }
        redirection.addFlashAttribute("message", "Réglages enregistrés");
        return "redirect:/admin/reglages";
    }

    @PostMapping("/{cle}/retablir")
    public String retablir(@PathVariable ReglageCle cle, RedirectAttributes redirection) {
        service.retablir(cle);
        redirection.addFlashAttribute("message", "Valeur par défaut rétablie");
        return "redirect:/admin/reglages";
    }

    private static List<LigneReglage> lignes(Map<ReglageCle, String> saisies, Map<ReglageCle, String> erreurs) {
        List<LigneReglage> lignes = new ArrayList<>();
        for (ReglageCle cle : ReglageCle.values()) {
            lignes.add(new LigneReglage(cle.name(), cle.libelle(), saisies.get(cle), cle.defaut(), cle.min(),
                    cle.max(), erreurs.get(cle)));
        }
        return lignes;
    }
}

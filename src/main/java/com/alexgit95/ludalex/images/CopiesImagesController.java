package com.alexgit95.ludalex.images;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/images")
public class CopiesImagesController {
    private final CopieImageRepository copies;
    private final CopiesImages service;
    private final ReferencesImages references;

    public CopiesImagesController(CopieImageRepository copies, CopiesImages service, ReferencesImages references) {
        this.copies = copies;
        this.service = service;
        this.references = references;
    }

    @GetMapping
    public String page(@RequestParam(defaultValue = "0") int page, Model model) {
        renseigner(page, model);
        return "admin/images";
    }

    @GetMapping("/etat")
    public String etat(@RequestParam(defaultValue = "0") int page, Model model) {
        renseigner(page, model);
        return "admin/images :: suivi";
    }

    @PostMapping("/relancer")
    public String relancer(@RequestParam(required = false) String cle,
            @RequestParam(defaultValue = "0") int page, Model model) {
        if (cle == null) { service.relancerEchecs(); }
        else { service.relancer(cle); }
        return etat(page, model);
    }

    private void renseigner(int numero, Model model) {
        var page = copies.findAll(PageRequest.of(Math.max(0, numero), 20, Sort.by("cle")));
        Map<String, Long> compteurs = new LinkedHashMap<>();
        for (var etat : CopieImage.Etat.values()) { compteurs.put(etat.name(), 0L); }
        long taille = 0;
        for (Object[] ligne : copies.compteurs()) {
            compteurs.put(((CopieImage.Etat) ligne[0]).name(), (Long) ligne[1]);
            taille += (Long) ligne[2];
        }
        model.addAttribute("page", page);
        model.addAttribute("compteurs", compteurs);
        model.addAttribute("octetsCopies", taille);
        model.addAttribute("fiches", references.fiches(page.getContent().stream().map(CopieImage::getUrl).toList()));
        model.addAttribute("libellesEtats", Map.of("EN_ATTENTE", "En attente", "EN_COURS", "En cours",
                "DISPONIBLE", "Disponible", "ECHEC", "En échec"));
    }
}
package com.alexgit95.ludalex.statistiques;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** Onglet Statistiques : page légère puis blocs chargés à la demande depuis le même instantané en cache. */
@Controller
@RequestMapping("/statistiques")
public class TableauDeBordController {

    static final List<String> BLOCS = List.of("kpi", "activite", "jeux", "joueurs", "ludotheque", "prets");

    private final StatistiquesGlobalesService service;

    public TableauDeBordController(StatistiquesGlobalesService service) {
        this.service = service;
    }

    @GetMapping
    public String page(@RequestParam(required = false) String periode, Model model) {
        model.addAttribute("periode", PeriodeStatistiques.depuisCode(periode));
        model.addAttribute("periodes", PeriodeStatistiques.values());
        model.addAttribute("blocs", BLOCS);
        return "statistiques/tableau";
    }

    @GetMapping("/bloc/{nom}")
    public String bloc(@PathVariable String nom, @RequestParam(required = false) String periode, Model model) {
        if (!BLOCS.contains(nom)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("stats", service.instantane(PeriodeStatistiques.depuisCode(periode)));
        return "statistiques/blocs :: " + nom;
    }
}

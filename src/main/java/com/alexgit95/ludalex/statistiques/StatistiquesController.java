package com.alexgit95.ludalex.statistiques;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.alexgit95.ludalex.jeux.JeuService;

@Controller
public class StatistiquesController {

    private final StatistiquesService statistiques;
    private final JeuService jeux;

    public StatistiquesController(StatistiquesService statistiques, JeuService jeux) {
        this.statistiques = statistiques;
        this.jeux = jeux;
    }

    /** Fragment HTMX chargé par la fiche d'un jeu. */
    @GetMapping("/jeux/{id}/statistiques")
    public String fiche(@PathVariable Long id, Model model) {
        jeux.trouver(id);
        model.addAttribute("stats", statistiques.pourJeu(id));
        return "statistiques/fiche :: statistiques";
    }
}

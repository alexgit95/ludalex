package com.alexgit95.ludalex.parties;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.jeux.JeuService;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.joueurs.JoueurService;

@Controller
@RequestMapping("/parties")
public class PartieController {

    private static final String FORMULAIRE = "parties/formulaire";

    private final PartieService parties;
    private final JeuService jeux;
    private final JoueurService joueurs;
    private final Clock horloge;

    public PartieController(PartieService parties, JeuService jeux, JoueurService joueurs, Clock horloge) {
        this.parties = parties;
        this.jeux = jeux;
        this.joueurs = joueurs;
        this.horloge = horloge;
    }

    @GetMapping
    public String liste(@RequestParam(required = false) Long jeu, @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("page", parties.lister(jeu, page));
        model.addAttribute("jeux", jeux.tousParNom());
        model.addAttribute("jeuFiltre", jeu);
        return "parties/liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(@RequestParam(required = false) Long jeu, Model model) {
        PartieForm form = new PartieForm();
        form.setDate(LocalDate.now(horloge));
        form.setJeuId(jeu);
        preparer(model, form, null, joueurs.selectionnables());
        return FORMULAIRE;
    }

    @PostMapping
    public String declarer(@ModelAttribute("form") PartieForm form, BindingResult erreurs, Model model,
            RedirectAttributes redirection) {
        if (!erreurs.hasErrors()) {
            try {
                parties.declarer(form.versSaisie());
                redirection.addFlashAttribute("message", "Partie enregistrée");
                return "redirect:/parties";
            } catch (PartieInvalideException e) {
                signaler(erreurs, e);
            }
        }
        preparer(model, form, null, joueurs.selectionnables());
        return FORMULAIRE;
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id, Model model) {
        DetailPartie detail = parties.detail(id);
        preparer(model, PartieForm.depuis(detail), detail, joueursProposes(detail));
        return FORMULAIRE;
    }

    @PostMapping("/{id}")
    public String enregistrer(@PathVariable Long id, @ModelAttribute("form") PartieForm form,
            BindingResult erreurs, Model model, RedirectAttributes redirection) {
        if (!erreurs.hasErrors()) {
            try {
                parties.modifier(id, form.versSaisie());
                redirection.addFlashAttribute("message", "Partie modifiée");
                return "redirect:/parties";
            } catch (PartieInvalideException e) {
                signaler(erreurs, e);
            }
        }
        DetailPartie detail = parties.detail(id);
        preparer(model, form, detail, joueursProposes(detail));
        return FORMULAIRE;
    }

    /** Fragment HTMX : zone de résultat selon que le jeu choisi est coopératif ou non. */
    @GetMapping("/resultat")
    public String resultat(@RequestParam(required = false) Long jeuId, Model model) {
        PartieForm form = new PartieForm();
        form.setJeuId(jeuId);
        model.addAttribute("form", form);
        model.addAttribute("joueurs", joueurs.selectionnables());
        model.addAttribute("cooperatif", estCooperatif(jeuId));
        model.addAttribute("jeuApercu", jeuChoisi(jeuId));
        model.addAttribute("equipesMax", PartieService.EQUIPES_MAX);
        return "parties/resultat :: resultat-et-apercu";
    }

    /** Fragment HTMX : lieux déjà saisis commençant par le texte tapé. */
    @GetMapping("/lieux")
    public String lieux(@RequestParam(required = false) String lieu, Model model) {
        model.addAttribute("lieux", parties.suggererLieux(lieu));
        return "parties/lieux :: lieux";
    }

    @GetMapping("/{id}/suppression")
    public String confirmation(@PathVariable Long id, Model model) {
        model.addAttribute("partie", parties.detail(id).partie());
        return "parties/confirmation :: confirmation";
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, RedirectAttributes redirection) {
        parties.supprimer(id);
        redirection.addFlashAttribute("message", "Partie supprimée");
        return "redirect:/parties";
    }

    private void preparer(Model model, PartieForm form, DetailPartie detail, List<Joueur> proposes) {
        model.addAttribute("form", form);
        model.addAttribute("detail", detail);
        model.addAttribute("action", detail == null ? "/parties" : "/parties/" + detail.partie().getId());
        model.addAttribute("jeux", jeux.tousParNom());
        model.addAttribute("joueurs", proposes);
        model.addAttribute("cooperatif", estCooperatif(form.getJeuId()));
        model.addAttribute("jeuApercu", jeuChoisi(form.getJeuId()));
        model.addAttribute("equipesMax", PartieService.EQUIPES_MAX);
    }

    /** Joueurs actifs, plus ceux déjà présents dans la partie (même devenus anonymes). */
    private List<Joueur> joueursProposes(DetailPartie detail) {
        List<Joueur> proposes = new ArrayList<>(joueurs.selectionnables());
        for (Participation participation : detail.participations()) {
            Joueur joueur = participation.getJoueur();
            if (proposes.stream().noneMatch(existant -> existant.getId().equals(joueur.getId()))) {
                proposes.add(joueur);
            }
        }
        return proposes;
    }

    private boolean estCooperatif(Long jeuId) {
        Jeu jeu = jeuChoisi(jeuId);
        return jeu != null && jeu.isCooperatif();
    }

    private Jeu jeuChoisi(Long jeuId) {
        return jeuId == null ? null : jeux.tousParNom().stream()
                .filter(jeu -> jeu.getId().equals(jeuId)).findFirst().orElse(null);
    }

    private static void signaler(BindingResult erreurs, PartieInvalideException e) {
        if ("date".equals(e.getChamp()) || "lieu".equals(e.getChamp())) {
            erreurs.rejectValue(e.getChamp(), "invalide", e.getMessage());
        } else {
            erreurs.reject("invalide", e.getMessage());
        }
    }
}

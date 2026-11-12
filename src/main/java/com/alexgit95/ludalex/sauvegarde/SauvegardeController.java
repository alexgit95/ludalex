package com.alexgit95.ludalex.sauvegarde;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alexgit95.ludalex.sauvegarde.DonneesSauvegarde.Manifeste;

/**
 * Page Admin de sauvegarde. Le ZIP est reçu comme corps brut de la requête (et non en multipart, limité à 2 Mo
 * pour les images) ; la page l'envoie avec un jeton CSRF et le mot de confirmation en en-têtes.
 */
@Controller
@RequestMapping("/admin/sauvegarde")
public class SauvegardeController {

    static final String MOT_CONFIRMATION = "RESTAURER";
    static final String ENTETE_CONFIRMATION = "X-Confirmation";

    private static final Logger LOG = LoggerFactory.getLogger(SauvegardeController.class);

    private final SauvegardeExport export;
    private final RestaurationSauvegarde restauration;
    private final Clock horloge;

    public SauvegardeController(SauvegardeExport export, RestaurationSauvegarde restauration, Clock horloge) {
        this.export = export;
        this.restauration = restauration;
        this.horloge = horloge;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("motConfirmation", MOT_CONFIRMATION);
        model.addAttribute("enteteConfirmation", ENTETE_CONFIRMATION);
        model.addAttribute("tailleMaxMo", restauration.tailleMax() / (1024 * 1024));
        return "admin/sauvegarde";
    }

    @GetMapping("/telecharger")
    public void telecharger(HttpServletResponse reponse) throws IOException {
        reponse.setContentType("application/zip");
        reponse.setHeader("Content-Disposition",
                "attachment; filename=\"ludalex-sauvegarde-" + LocalDate.now(horloge) + ".zip\"");
        reponse.setHeader("Cache-Control", "no-store");
        export.ecrire(reponse.getOutputStream());
    }

    @PostMapping("/restaurer")
    public String restaurer(@RequestHeader(name = ENTETE_CONFIRMATION, required = false) String confirmation,
            HttpServletRequest requete, HttpServletResponse reponse, Model model) {
        reponse.setHeader("Cache-Control", "no-store");
        if (confirmation == null || !MOT_CONFIRMATION.equals(confirmation.trim())) {
            return resultat(reponse, model, HttpServletResponse.SC_BAD_REQUEST,
                    "Confirmation manquante : saisissez " + MOT_CONFIRMATION + " pour remplacer toutes les données.");
        }
        if (requete.getContentLengthLong() > restauration.tailleMax()) {
            return resultat(reponse, model, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    new SauvegardeTropGrosseException(restauration.tailleMax()).getMessage());
        }
        try {
            Manifeste manifeste = restauration.restaurer(requete.getInputStream());
            model.addAttribute("succes", true);
            return resultat(reponse, model, HttpServletResponse.SC_OK, resume(manifeste));
        } catch (SauvegardeTropGrosseException e) {
            return resultat(reponse, model, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, e.getMessage());
        } catch (SauvegardeInvalideException e) {
            return resultat(reponse, model, HttpServletResponse.SC_BAD_REQUEST,
                    "Sauvegarde refusée, aucune donnée n'a été modifiée : " + e.getMessage());
        } catch (RestaurationEnCoursException e) {
            return resultat(reponse, model, HttpServletResponse.SC_CONFLICT, e.getMessage());
        } catch (IOException | RuntimeException e) {
            LOG.error("Échec de la restauration", e);
            return resultat(reponse, model, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "La restauration a échoué : consultez les journaux de l'application.");
        }
    }

    private static String resultat(HttpServletResponse reponse, Model model, int statut, String message) {
        reponse.setStatus(statut);
        model.addAttribute("message", message);
        return "admin/sauvegarde-resultat";
    }

    private static String resume(Manifeste manifeste) {
        Map<String, Long> c = manifeste.compteurs();
        return "Restauration terminée : " + c.get("joueurs") + " joueur(s), " + c.get("jeux") + " jeu(x), "
                + c.get("parties") + " partie(s), " + c.get("prets") + " prêt(s), " + c.get("images") + " image(s).";
    }
}

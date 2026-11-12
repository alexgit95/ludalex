package com.alexgit95.ludalex.importmyludo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Page Admin d'import MyLudo : envoi des fichiers, analyse, correspondance des joueurs, progression, rapport. */
@Controller
@RequestMapping("/admin/import-myludo")
public class ImportMyLudoController {

    private static final Logger LOG = LoggerFactory.getLogger(ImportMyLudoController.class);
    private static final String PAGE = "admin/import-myludo";
    private static final String REDIRECTION = "redirect:/admin/import-myludo";
    private static final String PREFIXE_CHOIX = "joueur-";

    private final ServiceImportMyLudo service;

    public ImportMyLudoController(ServiceImportMyLudo service) {
        this.service = service;
    }

    @GetMapping
    public String page(Model model, HttpServletResponse reponse) {
        reponse.setHeader("Cache-Control", "no-store");
        model.addAttribute("vue", service.vue());
        return PAGE;
    }

    @PostMapping("/analyser")
    public String analyser(@RequestParam(name = "collection", required = false) MultipartFile collection,
            @RequestParam(name = "parties", required = false) MultipartFile parties, RedirectAttributes flash) {
        try {
            service.demarrerAnalyse(octets(collection), nom(collection), octets(parties), nom(parties));
        } catch (ImportMyLudoException | ImportEnCoursException e) {
            flash.addFlashAttribute("message", e.getMessage());
        } catch (IOException e) {
            LOG.warn("Lecture impossible d'un fichier d'import MyLudo : {}", e.toString());
            flash.addFlashAttribute("message", "Un des fichiers n'a pas pu être lu");
        }
        return REDIRECTION;
    }

    @PostMapping("/importer")
    public String importer(@RequestParam Map<String, String> parametres, RedirectAttributes flash) {
        Map<Integer, String> choix = new HashMap<>();
        for (Map.Entry<String, String> parametre : parametres.entrySet()) {
            if (parametre.getKey().startsWith(PREFIXE_CHOIX)) {
                try {
                    choix.put(Integer.valueOf(parametre.getKey().substring(PREFIXE_CHOIX.length())),
                            parametre.getValue());
                } catch (NumberFormatException e) {
                    flash.addFlashAttribute("message", "Formulaire de correspondance invalide");
                    return REDIRECTION;
                }
            }
        }
        try {
            service.demarrerImport(choix);
        } catch (ImportMyLudoException | ImportEnCoursException e) {
            flash.addFlashAttribute("message", e.getMessage());
        }
        return REDIRECTION;
    }

    @PostMapping("/annuler")
    public String annuler() {
        service.abandonner();
        return REDIRECTION;
    }

    /** Fragment de suivi ; 204 tant que l'état connu de la page est à jour. */
    @GetMapping("/progression")
    public String progression(@RequestParam(required = false) String signature, Model model,
            HttpServletResponse reponse) {
        reponse.setHeader("Cache-Control", "no-store");
        ServiceImportMyLudo.Vue vue = service.vue();
        if (vue != null && vue.signature().equals(signature)) {
            reponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return "affichage/vide";
        }
        model.addAttribute("vue", vue);
        return "admin/import-myludo-zone :: zone";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String fichierTropVolumineux(Model model, HttpServletResponse reponse) {
        reponse.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        model.addAttribute("message", "Un fichier dépasse la taille maximale autorisée (2 Mo par fichier)");
        model.addAttribute("vue", service.vue());
        return PAGE;
    }

    private static byte[] octets(MultipartFile fichier) throws IOException {
        return fichier == null || fichier.isEmpty() ? null : fichier.getBytes();
    }

    private static String nom(MultipartFile fichier) {
        String nom = fichier == null ? null : fichier.getOriginalFilename();
        return nom == null || nom.isBlank() ? "fichier" : nom.replaceAll("[\\r\\n]", " ");
    }
}

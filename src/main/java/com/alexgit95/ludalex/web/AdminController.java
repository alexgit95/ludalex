package com.alexgit95.ludalex.web;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
public class AdminController {

    private final ObjectProvider<BuildProperties> build;

    public AdminController(ObjectProvider<BuildProperties> build) {
        this.build = build;
    }

    @GetMapping("/admin")
    public String accueil(Model model) {
        model.addAttribute("versionApplication", versionApplication(build.getIfAvailable()));
        return "admin/index";
    }

    static String versionApplication(BuildProperties build) {
        if (build == null) {
            return "inconnue";
        }
        String version = build.getVersion();
        String commit = build.get("git.commit");
        if (commit == null || commit.isBlank() || "unknown".equalsIgnoreCase(commit)) {
            return version;
        }
        return version + " - " + commit.substring(0, Math.min(7, commit.length()));
    }
}

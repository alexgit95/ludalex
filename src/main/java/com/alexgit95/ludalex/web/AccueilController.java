package com.alexgit95.ludalex.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AccueilController {

    @GetMapping("/")
    public String accueil() {
        return "accueil";
    }

    /** Le type MIME du manifeste n'est pas connu du serveur de ressources statiques. */
    @GetMapping(value = "/manifest.webmanifest", produces = "application/manifest+json")
    @ResponseBody
    public Resource manifeste() {
        return new ClassPathResource("static/manifest.webmanifest");
    }
}

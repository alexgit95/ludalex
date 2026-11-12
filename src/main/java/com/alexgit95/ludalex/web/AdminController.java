package com.alexgit95.ludalex.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin")
    public String accueil() {
        return "admin/index";
    }
}

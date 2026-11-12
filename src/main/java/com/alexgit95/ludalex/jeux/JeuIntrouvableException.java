package com.alexgit95.ludalex.jeux;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class JeuIntrouvableException extends RuntimeException {

    public JeuIntrouvableException(Long id) {
        super("Jeu introuvable : " + id);
    }
}

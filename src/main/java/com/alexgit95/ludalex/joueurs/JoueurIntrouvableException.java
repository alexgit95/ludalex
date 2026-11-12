package com.alexgit95.ludalex.joueurs;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class JoueurIntrouvableException extends RuntimeException {

    public JoueurIntrouvableException(Long id) {
        super("Joueur introuvable : " + id);
    }
}

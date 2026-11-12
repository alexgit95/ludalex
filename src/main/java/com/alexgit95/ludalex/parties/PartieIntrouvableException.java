package com.alexgit95.ludalex.parties;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PartieIntrouvableException extends RuntimeException {

    public PartieIntrouvableException(Long id) {
        super("Partie introuvable : " + id);
    }
}

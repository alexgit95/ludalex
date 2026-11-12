package com.alexgit95.ludalex.sauvegarde;

/** Le ZIP reçu est refusé ; le message est destiné à l'utilisateur et ne contient aucun détail interne. */
public class SauvegardeInvalideException extends RuntimeException {

    public SauvegardeInvalideException(String message) {
        super(message);
    }

    public SauvegardeInvalideException(String message, Throwable cause) {
        super(message, cause);
    }
}

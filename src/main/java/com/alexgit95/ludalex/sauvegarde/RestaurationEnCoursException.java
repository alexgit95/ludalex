package com.alexgit95.ludalex.sauvegarde;

public class RestaurationEnCoursException extends RuntimeException {

    public RestaurationEnCoursException() {
        super("Une restauration est déjà en cours");
    }
}

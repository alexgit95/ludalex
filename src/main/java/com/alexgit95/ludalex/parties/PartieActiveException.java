package com.alexgit95.ludalex.parties;

/** Une partie est déjà en cours ou en pause : il faut la terminer avant d'en lancer une autre. */
public class PartieActiveException extends RuntimeException {

    private final Long partieActiveId;

    public PartieActiveException(Long partieActiveId) {
        super("Une partie est déjà en cours");
        this.partieActiveId = partieActiveId;
    }

    public Long getPartieActiveId() {
        return partieActiveId;
    }
}

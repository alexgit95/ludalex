package com.alexgit95.ludalex.sauvegarde;

public class SauvegardeTropGrosseException extends SauvegardeInvalideException {

    public SauvegardeTropGrosseException(long maxOctets) {
        super("Le fichier dépasse la taille maximale autorisée (" + (maxOctets / (1024 * 1024)) + " Mo)");
    }
}

package com.alexgit95.ludalex.parties;

/** Token inconnu, expiré, déjà utilisé ou lié à une partie qui n'est plus en cours : un seul message pour tous. */
public class TokenFinInvalideException extends RuntimeException {

    public TokenFinInvalideException() {
        super("Lien invalide");
    }
}

package com.alexgit95.ludalex.images;

/** Image refusée ; le message est destiné à l'utilisateur. */
public class ImageInvalideException extends RuntimeException {

    public ImageInvalideException(String message) {
        super(message);
    }

    public ImageInvalideException(String message, Throwable cause) {
        super(message, cause);
    }
}

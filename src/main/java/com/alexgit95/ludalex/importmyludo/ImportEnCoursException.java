package com.alexgit95.ludalex.importmyludo;

public class ImportEnCoursException extends RuntimeException {

    public ImportEnCoursException() {
        super("Une analyse ou un import est déjà en cours");
    }
}

package com.alexgit95.ludalex.sauvegarde;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

/** Lecture en flux d'un tableau JSON d'un fichier de la sauvegarde, un enregistrement à la fois. */
final class LectureSauvegarde {

    private LectureSauvegarde() {
    }

    static <T> void lireTableau(ZipFile zip, String fichier, Class<T> type, UnaryOperator<InputStream> enveloppe,
            Consumer<T> action) {
        ZipEntry entree = zip.getEntry(fichier);
        if (entree == null) {
            throw new SauvegardeInvalideException("Fichier manquant dans la sauvegarde : " + fichier);
        }
        try (InputStream flux = enveloppe.apply(zip.getInputStream(entree));
                JsonParser lecteur = FormatSauvegarde.MAPPER_FLUX.createParser(flux)) {
            if (lecteur.nextToken() != JsonToken.START_ARRAY) {
                throw illisible(fichier, null);
            }
            while (lecteur.nextToken() == JsonToken.START_OBJECT) {
                action.accept(FormatSauvegarde.MAPPER_FLUX.readValue(lecteur, type));
            }
            if (lecteur.currentToken() != JsonToken.END_ARRAY || lecteur.nextToken() != null) {
                throw illisible(fichier, null);
            }
        } catch (JacksonException e) {
            SauvegardeInvalideException refus = causeInvalide(e);
            throw refus != null ? refus : illisible(fichier, e);
        } catch (IOException e) {
            SauvegardeInvalideException refus = causeInvalide(e);
            throw refus != null ? refus : illisible(fichier, e);
        }
    }

    private static SauvegardeInvalideException illisible(String fichier, Throwable cause) {
        return new SauvegardeInvalideException("Le fichier " + fichier + " est illisible ou mal formé", cause);
    }

    private static SauvegardeInvalideException causeInvalide(Throwable erreur) {
        for (Throwable courante = erreur; courante != null; courante = courante.getCause()) {
            if (courante instanceof SauvegardeInvalideException refus) {
                return refus;
            }
        }
        return null;
    }
}

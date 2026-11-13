package com.alexgit95.ludalex.sauvegarde;

import java.util.Map;

/**
 * Enregistrements du format de sauvegarde. Dates, instants et décimaux sont des chaînes (ISO-8601) pour rester
 * indépendants du moteur de base ; {@code idSauvegarde} n'est qu'une référence, jamais un identifiant réutilisé.
 */
final class DonneesSauvegarde {

    private DonneesSauvegarde() {
    }

    record Manifeste(Integer formatVersion, String appVersion, String date, Map<String, Long> compteurs) {
    }

    /** {@code type} vaut UPLOAD (référence = nom du fichier de {@code images/}) ou URL (référence = adresse). */
    record ImageDto(String type, String reference) {
    }

    record JoueurDto(Long idSauvegarde, String nom, ImageDto avatar, ImageDto imageVictoire, boolean anonymise) {
    }

    record JeuDto(Long idSauvegarde, String nom, ImageDto image, String couleurFond, String prix, String dateAchat,
            boolean cooperatif, Boolean plusieursVariantes) {
    }

    record PartieDto(Long idSauvegarde, Long jeuId, String datePartie, String lieu, String statut,
            String resultatCooperatif, Long joueurSuivantId, String termineeLe, String variante) {
    }

    record EquipeDto(Long idSauvegarde, Long partieId, String nom) {
    }

    record ParticipationDto(Long idSauvegarde, Long partieId, Long joueurId, Integer score, boolean gagnant,
            Long equipeId) {
    }

    record PretDto(Long idSauvegarde, Long jeuId, String emprunteur, String datePret, String dateRetour) {
    }
}

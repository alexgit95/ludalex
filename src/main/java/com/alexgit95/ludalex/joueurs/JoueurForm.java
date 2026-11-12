package com.alexgit95.ludalex.joueurs;

import com.alexgit95.ludalex.images.EntreeImage;
import org.springframework.web.multipart.MultipartFile;

/** Données du formulaire de joueur ; les noms des champs d'image suivent le fragment de saisie. */
public class JoueurForm {

    private String nom;

    private String avatarMode;
    private MultipartFile avatarFichier;
    private String avatarUrl;
    private boolean avatarRetirer;

    private String victoireMode;
    private MultipartFile victoireFichier;
    private String victoireUrl;
    private boolean victoireRetirer;

    EntreeImage avatar() {
        return entree(avatarMode, avatarFichier, avatarUrl, avatarRetirer);
    }

    EntreeImage victoire() {
        return entree(victoireMode, victoireFichier, victoireUrl, victoireRetirer);
    }

    private static EntreeImage entree(String mode, MultipartFile fichier, String url, boolean retirer) {
        if (retirer) {
            return new EntreeImage(null, null, true);
        }
        return "URL".equals(mode) ? new EntreeImage(null, url, false) : new EntreeImage(fichier, null, false);
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getAvatarMode() {
        return avatarMode;
    }

    public void setAvatarMode(String avatarMode) {
        this.avatarMode = avatarMode;
    }

    public MultipartFile getAvatarFichier() {
        return avatarFichier;
    }

    public void setAvatarFichier(MultipartFile avatarFichier) {
        this.avatarFichier = avatarFichier;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public boolean isAvatarRetirer() {
        return avatarRetirer;
    }

    public void setAvatarRetirer(boolean avatarRetirer) {
        this.avatarRetirer = avatarRetirer;
    }

    public String getVictoireMode() {
        return victoireMode;
    }

    public void setVictoireMode(String victoireMode) {
        this.victoireMode = victoireMode;
    }

    public MultipartFile getVictoireFichier() {
        return victoireFichier;
    }

    public void setVictoireFichier(MultipartFile victoireFichier) {
        this.victoireFichier = victoireFichier;
    }

    public String getVictoireUrl() {
        return victoireUrl;
    }

    public void setVictoireUrl(String victoireUrl) {
        this.victoireUrl = victoireUrl;
    }

    public boolean isVictoireRetirer() {
        return victoireRetirer;
    }

    public void setVictoireRetirer(boolean victoireRetirer) {
        this.victoireRetirer = victoireRetirer;
    }
}

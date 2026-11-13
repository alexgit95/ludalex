package com.alexgit95.ludalex.jeux;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import com.alexgit95.ludalex.images.EntreeImage;

/** Données du formulaire de jeu ; les champs d'image suivent le fragment de saisie. */
public class JeuForm {

    private String nom;
    private BigDecimal prix;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateAchat;

    private boolean cooperatif;
    private boolean plusieursVariantes;

    private String couleur;
    private boolean couleurManuelle;

    private String imageMode;
    private MultipartFile imageFichier;
    private String imageUrl;
    private boolean imageRetirer;

    JeuSaisie versSaisie() {
        EntreeImage image;
        if (imageRetirer) {
            image = new EntreeImage(null, null, true);
        } else if ("URL".equals(imageMode)) {
            image = new EntreeImage(null, imageUrl, false);
        } else {
            image = new EntreeImage(imageFichier, null, false);
        }
        return new JeuSaisie(nom, prix, dateAchat, cooperatif, couleurManuelle ? couleur : null, image,
            plusieursVariantes);
    }

    static JeuForm depuis(Jeu jeu) {
        JeuForm form = new JeuForm();
        form.nom = jeu.getNom();
        form.prix = jeu.getPrix();
        form.dateAchat = jeu.getDateAchat();
        form.cooperatif = jeu.isCooperatif();
        form.plusieursVariantes = jeu.isPlusieursVariantes();
        form.couleur = jeu.getCouleurFond();
        return form;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public LocalDate getDateAchat() {
        return dateAchat;
    }

    public void setDateAchat(LocalDate dateAchat) {
        this.dateAchat = dateAchat;
    }

    public boolean isCooperatif() {
        return cooperatif;
    }

    public void setCooperatif(boolean cooperatif) {
        this.cooperatif = cooperatif;
    }

    public boolean isPlusieursVariantes() {
        return plusieursVariantes;
    }

    public void setPlusieursVariantes(boolean plusieursVariantes) {
        this.plusieursVariantes = plusieursVariantes;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public boolean isCouleurManuelle() {
        return couleurManuelle;
    }

    public void setCouleurManuelle(boolean couleurManuelle) {
        this.couleurManuelle = couleurManuelle;
    }

    public String getImageMode() {
        return imageMode;
    }

    public void setImageMode(String imageMode) {
        this.imageMode = imageMode;
    }

    public MultipartFile getImageFichier() {
        return imageFichier;
    }

    public void setImageFichier(MultipartFile imageFichier) {
        this.imageFichier = imageFichier;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isImageRetirer() {
        return imageRetirer;
    }

    public void setImageRetirer(boolean imageRetirer) {
        this.imageRetirer = imageRetirer;
    }
}

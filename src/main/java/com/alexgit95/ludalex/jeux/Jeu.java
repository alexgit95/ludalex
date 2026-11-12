package com.alexgit95.ludalex.jeux;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.alexgit95.ludalex.images.CalculateurCouleurFond;
import com.alexgit95.ludalex.images.ImageSource;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "jeu")
public class Jeu {

    public static final int NOM_MAX = 200;
    public static final BigDecimal PRIX_MAX = new BigDecimal("99999999.99");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = NOM_MAX)
    private String nom;

    @Embedded
    private ImageSource image;

    @Column(name = "couleur_fond", nullable = false, length = 7)
    private String couleurFond = CalculateurCouleurFond.NEUTRE;

    @Column(precision = 10, scale = 2)
    private BigDecimal prix;

    @Column(name = "date_achat")
    private LocalDate dateAchat;

    @Column(nullable = false)
    private boolean cooperatif;

    protected Jeu() {
    }

    public Jeu(String nom) {
        this.nom = nom;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public ImageSource getImage() {
        return image;
    }

    public void setImage(ImageSource image) {
        this.image = image;
    }

    public String getCouleurFond() {
        return couleurFond;
    }

    /** N'accepte qu'une couleur #RRGGBB : la valeur est injectée dans un style CSS. */
    public void setCouleurFond(String couleurFond) {
        this.couleurFond = CalculateurCouleurFond.estCouleurValide(couleurFond)
                ? couleurFond : CalculateurCouleurFond.NEUTRE;
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
}

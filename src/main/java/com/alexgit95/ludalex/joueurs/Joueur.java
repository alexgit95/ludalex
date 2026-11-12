package com.alexgit95.ludalex.joueurs;

import java.util.Locale;

import com.alexgit95.ludalex.images.ImageSource;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fiche de joueur déclarée dans l'application, sans compte. */
@Entity
@Table(name = "joueur")
public class Joueur {

    public static final String NOM_ANONYME = "Joueur supprimé";
    public static final int NOM_MAX = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = NOM_MAX)
    private String nom;

    /** Nom en minuscules, pour une unicité indépendante du moteur de base. */
    @Column(name = "nom_normalise", nullable = false, length = NOM_MAX)
    private String nomNormalise;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "type", column = @Column(name = "avatar_type", length = 10)),
            @AttributeOverride(name = "reference", column = @Column(name = "avatar_reference", length = 2048))
    })
    private ImageSource avatar;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "type", column = @Column(name = "victoire_type", length = 10)),
            @AttributeOverride(name = "reference", column = @Column(name = "victoire_reference", length = 2048))
    })
    private ImageSource imageVictoire;

    @Column(nullable = false)
    private boolean anonymise;

    protected Joueur() {
    }

    public Joueur(String nom) {
        changerNom(nom);
    }

    public void changerNom(String nouveauNom) {
        this.nom = nouveauNom;
        this.nomNormalise = nouveauNom.toLowerCase(Locale.ROOT);
    }

    /** Efface l'identité du joueur en gardant sa fiche, pour conserver l'historique des parties. */
    public void anonymiser() {
        changerNom(NOM_ANONYME);
        this.avatar = null;
        this.imageVictoire = null;
        this.anonymise = true;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public ImageSource getAvatar() {
        return avatar;
    }

    public void setAvatar(ImageSource avatar) {
        this.avatar = avatar;
    }

    public ImageSource getImageVictoire() {
        return imageVictoire;
    }

    public void setImageVictoire(ImageSource imageVictoire) {
        this.imageVictoire = imageVictoire;
    }

    public boolean isAnonymise() {
        return anonymise;
    }
}

package com.alexgit95.ludalex.reglages;

import com.alexgit95.ludalex.donnees.ChangementDonnees;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Réglage persisté sous forme clé/valeur ; seules les valeurs modifiées sont stockées. */
@Entity
@EntityListeners(ChangementDonnees.class)
@Table(name = "reglage", uniqueConstraints = @UniqueConstraint(name = "uk_reglage_cle", columnNames = "cle"))
public class Reglage {

    public static final int CLE_MAX = 100;
    public static final int VALEUR_MAX = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = CLE_MAX)
    private String cle;

    @Column(nullable = false, length = VALEUR_MAX)
    private String valeur;

    protected Reglage() {
    }

    public Reglage(String cle, String valeur) {
        this.cle = cle;
        this.valeur = valeur;
    }

    public Long getId() {
        return id;
    }

    public String getCle() {
        return cle;
    }

    public String getValeur() {
        return valeur;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }
}

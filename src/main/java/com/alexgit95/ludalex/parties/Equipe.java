package com.alexgit95.ludalex.parties;

import com.alexgit95.ludalex.donnees.ChangementDonnees;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Équipe propre à une partie : elle n'existe que pour la partie qui la définit. */
@Entity
@EntityListeners(ChangementDonnees.class)
@Table(name = "equipe")
public class Equipe {

    public static final int NOM_MAX = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partie_id", nullable = false)
    private Partie partie;

    @Column(nullable = false, length = NOM_MAX)
    private String nom;

    protected Equipe() {
    }

    public Equipe(Partie partie, String nom) {
        this.partie = partie;
        this.nom = nom;
    }

    public Long getId() {
        return id;
    }

    public Partie getPartie() {
        return partie;
    }

    public String getNom() {
        return nom;
    }
}

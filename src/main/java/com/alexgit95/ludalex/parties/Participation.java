package com.alexgit95.ludalex.parties;

import com.alexgit95.ludalex.joueurs.Joueur;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Présence d'un joueur dans une partie : score facultatif et victoire. */
@Entity
@Table(name = "participation",
        uniqueConstraints = @UniqueConstraint(name = "uk_participation", columnNames = { "partie_id", "joueur_id" }),
        indexes = { @Index(name = "idx_participation_partie", columnList = "partie_id"),
                @Index(name = "idx_participation_joueur", columnList = "joueur_id") })
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partie_id", nullable = false)
    private Partie partie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "joueur_id", nullable = false)
    private Joueur joueur;

    @Column
    private Integer score;

    @Column(nullable = false)
    private boolean gagnant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id")
    private Equipe equipe;

    protected Participation() {
    }

    public Participation(Partie partie, Joueur joueur) {
        this.partie = partie;
        this.joueur = joueur;
    }

    public Long getId() {
        return id;
    }

    public Partie getPartie() {
        return partie;
    }

    public Joueur getJoueur() {
        return joueur;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public boolean isGagnant() {
        return gagnant;
    }

    public void setGagnant(boolean gagnant) {
        this.gagnant = gagnant;
    }

    public Equipe getEquipe() {
        return equipe;
    }

    public void setEquipe(Equipe equipe) {
        this.equipe = equipe;
    }
}

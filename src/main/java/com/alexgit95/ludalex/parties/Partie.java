package com.alexgit95.ludalex.parties;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.joueurs.Joueur;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "partie", indexes = @Index(name = "idx_partie_jeu", columnList = "jeu_id, statut"))
public class Partie {

    public static final int LIEU_MAX = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jeu_id", nullable = false)
    private Jeu jeu;

    @Column(name = "date_partie", nullable = false)
    private LocalDate datePartie;

    @Column(length = LIEU_MAX)
    private String lieu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatutPartie statut = StatutPartie.TERMINEE;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultat_cooperatif", length = 10)
    private ResultatCooperatif resultatCooperatif;

    /** Joueur à qui c'était de jouer lors d'une pause (utilisé par les parties en direct). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "joueur_suivant_id")
    private Joueur joueurSuivant;

    /** Instant de fin d'une partie en direct ; vide pour une partie déclarée après coup. */
    @Column(name = "terminee_le")
    private Instant termineeLe;

    @OneToMany(mappedBy = "partie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participation> participations = new ArrayList<>();

    @OneToMany(mappedBy = "partie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Equipe> equipes = new ArrayList<>();

    protected Partie() {
    }

    public Partie(Jeu jeu, LocalDate datePartie) {
        this.jeu = jeu;
        this.datePartie = datePartie;
    }

    public Long getId() {
        return id;
    }

    public Jeu getJeu() {
        return jeu;
    }

    public void setJeu(Jeu jeu) {
        this.jeu = jeu;
    }

    public LocalDate getDatePartie() {
        return datePartie;
    }

    public void setDatePartie(LocalDate datePartie) {
        this.datePartie = datePartie;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public StatutPartie getStatut() {
        return statut;
    }

    public void setStatut(StatutPartie statut) {
        this.statut = statut;
    }

    public ResultatCooperatif getResultatCooperatif() {
        return resultatCooperatif;
    }

    public void setResultatCooperatif(ResultatCooperatif resultatCooperatif) {
        this.resultatCooperatif = resultatCooperatif;
    }

    public Joueur getJoueurSuivant() {
        return joueurSuivant;
    }

    public void setJoueurSuivant(Joueur joueurSuivant) {
        this.joueurSuivant = joueurSuivant;
    }

    public Instant getTermineeLe() {
        return termineeLe;
    }

    public void setTermineeLe(Instant termineeLe) {
        this.termineeLe = termineeLe;
    }

    public List<Participation> getParticipations() {
        return participations;
    }

    public List<Equipe> getEquipes() {
        return equipes;
    }
}

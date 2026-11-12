package com.alexgit95.ludalex.jeux;

import java.time.LocalDate;

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

/** Prêt d'un jeu ; en cours tant que la date de retour est vide. */
@Entity
@Table(name = "pret", indexes = @Index(name = "idx_pret_jeu", columnList = "jeu_id"))
public class Pret {

    public static final int EMPRUNTEUR_MAX = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jeu_id", nullable = false)
    private Jeu jeu;

    @Column(nullable = false, length = EMPRUNTEUR_MAX)
    private String emprunteur;

    @Column(name = "date_pret", nullable = false)
    private LocalDate datePret;

    @Column(name = "date_retour")
    private LocalDate dateRetour;

    protected Pret() {
    }

    public Pret(Jeu jeu, String emprunteur, LocalDate datePret) {
        this.jeu = jeu;
        this.emprunteur = emprunteur;
        this.datePret = datePret;
    }

    public Long getId() {
        return id;
    }

    public String getEmprunteur() {
        return emprunteur;
    }

    public LocalDate getDatePret() {
        return datePret;
    }

    public LocalDate getDateRetour() {
        return dateRetour;
    }

    public void clore(LocalDate dateRetour) {
        this.dateRetour = dateRetour;
    }

    public boolean estEnCours() {
        return dateRetour == null;
    }
}

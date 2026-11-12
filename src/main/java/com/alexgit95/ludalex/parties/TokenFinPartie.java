package com.alexgit95.ludalex.parties;

import java.time.Instant;

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

/** Autorisation de fin de partie : seule l'empreinte du token est stockée, jamais le token. */
@Entity
@Table(name = "token_fin_partie", indexes = @Index(name = "uk_token_empreinte", columnList = "empreinte", unique = true))
public class TokenFinPartie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partie_id", nullable = false)
    private Partie partie;

    @Column(nullable = false, length = 64)
    private String empreinte;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    @Column(nullable = false)
    private boolean utilise;

    protected TokenFinPartie() {
    }

    public TokenFinPartie(Partie partie, String empreinte, Instant expireLe) {
        this.partie = partie;
        this.empreinte = empreinte;
        this.expireLe = expireLe;
    }

    public Long getId() {
        return id;
    }

    public Partie getPartie() {
        return partie;
    }

    public String getEmpreinte() {
        return empreinte;
    }

    public Instant getExpireLe() {
        return expireLe;
    }

    public boolean isUtilise() {
        return utilise;
    }
}

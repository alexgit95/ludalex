package com.alexgit95.ludalex.images;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "copie_image")
public class CopieImage {
    public enum Etat { EN_ATTENTE, EN_COURS, DISPONIBLE, ECHEC }

    @Id
    @Column(length = 64)
    private String cle;
    @Column(nullable = false, length = 2048)
    private String url;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Etat etat = Etat.EN_ATTENTE;
    @Column(length = 64)
    private String fichier;
    private long taille;
    private int tentatives;
    private Instant derniereTentative;
    private Instant prochaineTentative;
    @Column(length = 200)
    private String erreur;

    protected CopieImage() { }

    public CopieImage(String url) {
        this.url = url;
        this.cle = cle(url);
    }

    public static String cle(String url) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(url.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public void commencer(Instant maintenant) {
        etat = Etat.EN_COURS;
        tentatives++;
        derniereTentative = maintenant;
        prochaineTentative = null;
        erreur = null;
    }

    public void disponible(String nom, long octets) {
        fichier = nom;
        taille = octets;
        etat = Etat.DISPONIBLE;
        prochaineTentative = null;
        erreur = null;
    }

    public void echouer(Instant maintenant, boolean definitif, String message) {
        erreur = message == null ? "Image inaccessible" : message.substring(0, Math.min(200, message.length()));
        etat = definitif || tentatives >= 3 ? Etat.ECHEC : Etat.EN_ATTENTE;
        prochaineTentative = etat == Etat.EN_ATTENTE
                ? maintenant.plusSeconds(tentatives <= 1 ? 60 : 900) : null;
    }

    public void reprendre() {
        etat = Etat.EN_ATTENTE;
        prochaineTentative = null;
    }

    public void relancer() {
        reprendre();
        fichier = null;
        taille = 0;
        tentatives = 0;
        erreur = null;
    }

    public String getCle() { return cle; }
    public String getUrl() { return url; }
    public Etat getEtat() { return etat; }
    public String getFichier() { return fichier; }
    public long getTaille() { return taille; }
    public int getTentatives() { return tentatives; }
    public Instant getDerniereTentative() { return derniereTentative; }
    public Instant getProchaineTentative() { return prochaineTentative; }
    public String getErreur() { return erreur; }
}
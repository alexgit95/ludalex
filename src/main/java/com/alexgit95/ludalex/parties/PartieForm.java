package com.alexgit95.ludalex.parties;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.format.annotation.DateTimeFormat;

/** Données du formulaire de partie ; les noms des champs suivent les gabarits (scores[id], equipeDe[id]...). */
public class PartieForm {

    private Long jeuId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    private String lieu;
    private List<Long> joueurIds = new ArrayList<>();
    private Map<Long, Integer> scores = new LinkedHashMap<>();
    private List<Long> gagnantIds = new ArrayList<>();
    private Map<Long, Integer> equipeDe = new LinkedHashMap<>();
    private Map<Integer, String> equipeNoms = new TreeMap<>();
    private List<Integer> equipesGagnantes = new ArrayList<>();
    private ResultatCooperatif resultat;

    PartieSaisie versSaisie() {
        return new PartieSaisie(jeuId, date, lieu, joueurIds, scores, gagnantIds, equipeDe, equipeNoms,
                equipesGagnantes, resultat);
    }

    static PartieForm depuis(DetailPartie detail) {
        PartieForm form = new PartieForm();
        Partie partie = detail.partie();
        form.jeuId = partie.getJeu().getId();
        form.date = partie.getDatePartie();
        form.lieu = partie.getLieu();
        form.resultat = partie.getResultatCooperatif();
        Map<Long, Integer> numeros = new LinkedHashMap<>();
        for (int i = 0; i < detail.equipes().size(); i++) {
            Equipe equipe = detail.equipes().get(i);
            numeros.put(equipe.getId(), i + 1);
            form.equipeNoms.put(i + 1, equipe.getNom());
        }
        for (Participation participation : detail.participations()) {
            Long joueurId = participation.getJoueur().getId();
            form.joueurIds.add(joueurId);
            form.scores.put(joueurId, participation.getScore());
            if (participation.isGagnant()) {
                form.gagnantIds.add(joueurId);
            }
            if (participation.getEquipe() != null) {
                form.equipeDe.put(joueurId, numeros.get(participation.getEquipe().getId()));
            }
        }
        return form;
    }

    public Long getJeuId() {
        return jeuId;
    }

    public void setJeuId(Long jeuId) {
        this.jeuId = jeuId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public List<Long> getJoueurIds() {
        return joueurIds;
    }

    public void setJoueurIds(List<Long> joueurIds) {
        this.joueurIds = joueurIds;
    }

    public Map<Long, Integer> getScores() {
        return scores;
    }

    public void setScores(Map<Long, Integer> scores) {
        this.scores = scores;
    }

    public List<Long> getGagnantIds() {
        return gagnantIds;
    }

    public void setGagnantIds(List<Long> gagnantIds) {
        this.gagnantIds = gagnantIds;
    }

    public Map<Long, Integer> getEquipeDe() {
        return equipeDe;
    }

    public void setEquipeDe(Map<Long, Integer> equipeDe) {
        this.equipeDe = equipeDe;
    }

    public Map<Integer, String> getEquipeNoms() {
        return equipeNoms;
    }

    public void setEquipeNoms(Map<Integer, String> equipeNoms) {
        this.equipeNoms = equipeNoms;
    }

    public List<Integer> getEquipesGagnantes() {
        return equipesGagnantes;
    }

    public void setEquipesGagnantes(List<Integer> equipesGagnantes) {
        this.equipesGagnantes = equipesGagnantes;
    }

    public ResultatCooperatif getResultat() {
        return resultat;
    }

    public void setResultat(ResultatCooperatif resultat) {
        this.resultat = resultat;
    }
}

package com.alexgit95.ludalex.parties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Champs de fin de partie, partagés par l'écran du propriétaire et la page publique du lien. */
public class FinForm {

    private List<Long> gagnantIds = new ArrayList<>();
    private List<Long> equipesGagnantes = new ArrayList<>();
    private Map<Long, Integer> scores = new LinkedHashMap<>();
    private ResultatCooperatif resultat;
    private Long joueurSuivantId;

    FinSaisie versSaisie() {
        return new FinSaisie(gagnantIds, equipesGagnantes, scores, resultat);
    }

    public List<Long> getGagnantIds() {
        return gagnantIds;
    }

    public void setGagnantIds(List<Long> gagnantIds) {
        this.gagnantIds = gagnantIds;
    }

    public List<Long> getEquipesGagnantes() {
        return equipesGagnantes;
    }

    public void setEquipesGagnantes(List<Long> equipesGagnantes) {
        this.equipesGagnantes = equipesGagnantes;
    }

    public Map<Long, Integer> getScores() {
        return scores;
    }

    public void setScores(Map<Long, Integer> scores) {
        this.scores = scores;
    }

    public ResultatCooperatif getResultat() {
        return resultat;
    }

    public void setResultat(ResultatCooperatif resultat) {
        this.resultat = resultat;
    }

    public Long getJoueurSuivantId() {
        return joueurSuivantId;
    }

    public void setJoueurSuivantId(Long joueurSuivantId) {
        this.joueurSuivantId = joueurSuivantId;
    }
}

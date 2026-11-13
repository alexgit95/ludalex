package com.alexgit95.ludalex.parties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Champs du formulaire de lancement d'une partie en direct. */
public class LancementForm {

    private Long jeuId;
    private String variante;
    private List<Long> joueurIds = new ArrayList<>();
    private Map<Long, Integer> equipeDe = new LinkedHashMap<>();
    private Map<Integer, String> equipeNoms = new TreeMap<>();

    LancementSaisie versSaisie() {
        return new LancementSaisie(jeuId, joueurIds, equipeDe, equipeNoms, variante);
    }

    public Long getJeuId() {
        return jeuId;
    }

    public void setJeuId(Long jeuId) {
        this.jeuId = jeuId;
    }

    public String getVariante() {
        return variante;
    }

    public void setVariante(String variante) {
        this.variante = variante;
    }

    public List<Long> getJoueurIds() {
        return joueurIds;
    }

    public void setJoueurIds(List<Long> joueurIds) {
        this.joueurIds = joueurIds;
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
}

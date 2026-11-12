package com.alexgit95.ludalex.importmyludo;

import java.util.concurrent.atomic.AtomicLong;

/** Avancement d'une analyse ou d'un import, lu par la page pendant que le traitement s'exécute. */
public class Progression {

    public enum Phase {
        LECTURE("Lecture des fichiers"), ANALYSE("Analyse"), IMPORT("Import");

        private final String libelle;

        Phase(String libelle) {
            this.libelle = libelle;
        }

        public String libelle() {
            return libelle;
        }
    }

    private volatile Phase phase = Phase.LECTURE;
    private volatile long total;
    private volatile String courant = "";
    private final AtomicLong fait = new AtomicLong();

    public void demarrer(Phase nouvellePhase, long nouveauTotal) {
        this.phase = nouvellePhase;
        this.total = nouveauTotal;
        this.fait.set(0);
        this.courant = "";
    }

    public void avancer(String element) {
        fait.incrementAndGet();
        this.courant = element == null ? "" : element;
    }

    public Phase phase() {
        return phase;
    }

    public long total() {
        return total;
    }

    public long fait() {
        return fait.get();
    }

    public String courant() {
        return courant;
    }

    public String signature() {
        return phase + ":" + fait.get() + ":" + total + ":" + courant;
    }
}

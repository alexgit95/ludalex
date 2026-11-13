package com.alexgit95.ludalex.images;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Interrupteur en mémoire de la récupération des copies : chaque démarrage repart en pause sauf configuration. */
@Component
public class EtatCopies {

    private final AtomicBoolean actif;
    private final boolean traitementConfigure;

    public EtatCopies(@Value("${ludalex.images.copies.demarrage-actif:false}") boolean demarrageActif,
            @Value("${ludalex.images.copies.actif:true}") boolean traitementConfigure) {
        this.actif = new AtomicBoolean(demarrageActif);
        this.traitementConfigure = traitementConfigure;
    }

    public boolean actif() {
        return actif.get();
    }

    public void activer() {
        actif.set(true);
    }

    public void mettreEnPause() {
        actif.set(false);
    }

    /** Faux si {@code ludalex.images.copies.actif=false} : aucun traitement n'est alors planifié. */
    public boolean traitementConfigure() {
        return traitementConfigure;
    }
}

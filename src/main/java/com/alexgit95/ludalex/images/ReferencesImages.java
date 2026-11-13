package com.alexgit95.ludalex.images;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;

@Repository
public class ReferencesImages {
    private static final List<String> SOURCES = List.of("Jeu:image", "Joueur:avatar", "Joueur:imageVictoire");
    private final EntityManager em;

    public ReferencesImages(EntityManager em) { this.em = em; }

    public void parcourir(Consumer<String> accepter) {
        for (String source : SOURCES) {
            String[] morceaux = source.split(":");
            long dernier = 0;
            while (true) {
                List<Object[]> lot = em.createQuery("select x.id, x." + morceaux[1] + ".reference from "
                        + morceaux[0] + " x where x.id > :apres and x." + morceaux[1]
                        + ".type = :type order by x.id", Object[].class)
                        .setParameter("apres", dernier).setParameter("type", TypeImage.URL).setMaxResults(100)
                        .getResultList();
                for (Object[] ligne : lot) {
                    dernier = (Long) ligne[0];
                    accepter.accept((String) ligne[1]);
                }
                if (lot.size() < 100) { break; }
            }
        }
    }

    public boolean existe(String reference, TypeImage type) {
        for (String source : SOURCES) {
            String[] morceaux = source.split(":");
            if (!em.createQuery("select x.id from " + morceaux[0] + " x where x." + morceaux[1]
                    + ".type = :type and x." + morceaux[1] + ".reference = :reference", Long.class)
                    .setParameter("type", type).setParameter("reference", reference).setMaxResults(1)
                    .getResultList().isEmpty()) { return true; }
        }
        return false;
    }

    public Map<String, List<Fiche>> fiches(List<String> urls) {
        Map<String, List<Fiche>> resultat = new LinkedHashMap<>();
        if (urls.isEmpty()) { return resultat; }
        for (String source : SOURCES) {
            String[] morceaux = source.split(":");
            List<Object[]> lignes = em.createQuery("select x." + morceaux[1] + ".reference, x.id, x.nom from "
                    + morceaux[0] + " x where x." + morceaux[1] + ".type = :type and x." + morceaux[1]
                    + ".reference in :urls order by x.id", Object[].class)
                    .setParameter("type", TypeImage.URL).setParameter("urls", urls).setMaxResults(200).getResultList();
            for (Object[] ligne : lignes) {
                resultat.computeIfAbsent((String) ligne[0], ignore -> new ArrayList<>())
                        .add(new Fiche((String) ligne[2], (morceaux[0].equals("Jeu") ? "/jeux/" : "/admin/joueurs/")
                                + ligne[1] + (morceaux[0].equals("Jeu") ? "" : "/modifier")));
            }
        }
        return resultat;
    }

    public record Fiche(String nom, String lien) { }
}
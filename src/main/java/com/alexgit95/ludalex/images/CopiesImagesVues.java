package com.alexgit95.ludalex.images;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.alexgit95.ludalex.jeux.Jeu;
import com.alexgit95.ludalex.joueurs.Joueur;
import com.alexgit95.ludalex.parties.Partie;
import com.alexgit95.ludalex.parties.Participation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class CopiesImagesVues implements WebMvcConfigurer, HandlerInterceptor {
    private final CopiesImages copies;
    public CopiesImagesVues(CopiesImages copies) { this.copies = copies; }

    @Override
    public void addInterceptors(InterceptorRegistry registre) {
        registre.addInterceptor(this).excludePathPatterns("/fin/**");
    }

    @Override
    public void postHandle(HttpServletRequest requete, HttpServletResponse reponse, Object gestionnaire,
            ModelAndView vue) {
        if (vue == null || vue.getViewName() == null || vue.getViewName().startsWith("redirect:")) { return; }
        Set<String> urls = new LinkedHashSet<>();
        Set<Object> visites = Collections.newSetFromMap(new IdentityHashMap<>());
        collecter(vue.getModel(), urls, visites);
        Map<String, String> disponibles = new java.util.LinkedHashMap<>();
        var liste = new ArrayList<>(urls);
        for (int debut = 0; debut < liste.size(); debut += 100) {
            disponibles.putAll(copies.disponibles(liste.subList(debut, Math.min(debut + 100, liste.size()))));
        }
        vue.addObject("copiesImages", disponibles);
    }

    private void collecter(Object objet, Set<String> urls, Set<Object> visites) {
        if (objet == null || !org.hibernate.Hibernate.isInitialized(objet) || !visites.add(objet)) { return; }
        if (objet instanceof ImageSource image) {
            if (image.getType() == TypeImage.URL) { urls.add(image.getReference()); }
        } else if (objet instanceof Jeu jeu) {
            collecter(jeu.getImage(), urls, visites);
        } else if (objet instanceof Joueur joueur) {
            collecter(joueur.getAvatar(), urls, visites);
            collecter(joueur.getImageVictoire(), urls, visites);
        } else if (objet instanceof Participation participation) {
            collecter(participation.getJoueur(), urls, visites);
        } else if (objet instanceof Partie partie) {
            collecter(partie.getJeu(), urls, visites);
            collecter(partie.getJoueurSuivant(), urls, visites);
        } else if (objet instanceof Page<?> page) {
            collecter(page.getContent(), urls, visites);
        } else if (objet instanceof Map<?, ?> map) {
            map.values().forEach(valeur -> collecter(valeur, urls, visites));
        } else if (objet instanceof Collection<?> collection) {
            collection.forEach(valeur -> collecter(valeur, urls, visites));
        } else if (objet instanceof Optional<?> option) {
            option.ifPresent(valeur -> collecter(valeur, urls, visites));
        } else if (objet.getClass().isRecord() && objet.getClass().getPackageName().startsWith("com.alexgit95.ludalex")) {
            for (var composant : objet.getClass().getRecordComponents()) {
                try { collecter(composant.getAccessor().invoke(objet), urls, visites); }
                catch (ReflectiveOperationException exception) { throw new IllegalStateException("Modele de vue inaccessible", exception); }
            }
        }
    }
}
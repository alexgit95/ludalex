package com.alexgit95.ludalex.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.servlet.ModelAndView;
import com.alexgit95.ludalex.jeux.Jeu;

class CopiesImagesVuesTest {
    public record Ligne(Jeu jeu) { }

    @Test
    void resoutUnePageParLotSansRequeteParImage() {
        CopiesImages copies = mock(CopiesImages.class);
        Jeu jeu = new Jeu("Jeu");
        jeu.setImage(ImageSource.url("https://example.org/a.png"));
        when(copies.disponibles(List.of(jeu.getImage().getUrl())))
                .thenReturn(Map.of(jeu.getImage().getUrl(), "/images/copie.png"));
        ModelAndView vue = new ModelAndView("jeux/liste");
        vue.addObject("page", new PageImpl<>(List.of(new Ligne(jeu), new Ligne(jeu))));
        new CopiesImagesVues(copies).postHandle(null, null, null, vue);
        assertThat(vue.getModel().get("copiesImages")).isEqualTo(Map.of(jeu.getImage().getUrl(), "/images/copie.png"));
        verify(copies, times(1)).disponibles(anyList());
    }

    @Test
    void ignoreUneRedirection() {
        CopiesImages copies = mock(CopiesImages.class);
        new CopiesImagesVues(copies).postHandle(null, null, null, new ModelAndView("redirect:/jeux"));
        verifyNoInteractions(copies);
    }

    @Test
    void neChargePasUnProxyJpaPourChercherDesImages() {
        CopiesImages copies = mock(CopiesImages.class);
        Jeu jeu = mock(Jeu.class, withSettings().extraInterfaces(org.hibernate.proxy.HibernateProxy.class)
                .defaultAnswer(CALLS_REAL_METHODS));
        var initialiseur = mock(org.hibernate.proxy.LazyInitializer.class);
        when(((org.hibernate.proxy.HibernateProxy) jeu).getHibernateLazyInitializer()).thenReturn(initialiseur);
        when(initialiseur.isUninitialized()).thenReturn(true);
        ModelAndView vue = new ModelAndView("statistiques/fiche");
        vue.addObject("jeu", jeu);
        new CopiesImagesVues(copies).postHandle(null, null, null, vue);
        verify(jeu, never()).getImage();
        verifyNoInteractions(copies);
    }
}
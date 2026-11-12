package com.alexgit95.ludalex.parties;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.Order;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.time.Clock;

/**
 * Protège les liens publics {@code /fin/**} : limitation de débit par adresse, avant Spring Security
 * et avant tout accès à la base, et en-têtes interdisant cache, fuite du lien et indexation.
 */
@Component
@Order(-200)
public class ProtectionFinPartieFilter extends OncePerRequestFilter {

    private record Fenetre(long minute, int compteur) {
    }

    private final RequestMatcher cheminFin = PathPatternRequestMatcher.withDefaults().matcher("/fin/**");
    private final ConcurrentHashMap<String, Fenetre> fenetres = new ConcurrentHashMap<>();
    private final FinPartieProperties proprietes;
    private final Clock horloge;

    public ProtectionFinPartieFilter(FinPartieProperties proprietes, Clock horloge) {
        this.proprietes = proprietes;
        this.horloge = horloge;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !cheminFin.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chaine)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Robots-Tag", "noindex");
        if (limiteAtteinte(adresse(request))) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Trop de requêtes");
            return;
        }
        chaine.doFilter(request, response);
    }

    private boolean limiteAtteinte(String adresse) {
        long minute = horloge.millis() / 60_000;
        Fenetre fenetre = fenetres.compute(adresse, (cle, actuelle) -> actuelle == null || actuelle.minute() != minute
                ? new Fenetre(minute, 1) : new Fenetre(minute, actuelle.compteur() + 1));
        if (fenetres.size() > 1000) {
            fenetres.values().removeIf(ancienne -> ancienne.minute() < minute);
        }
        return fenetre.compteur() > proprietes.limiteParMinute();
    }

    /** Adresse du client : l'en-tête de confiance configuré (Cloudflare) s'il est présent, sinon la connexion. */
    private String adresse(HttpServletRequest request) {
        if (StringUtils.hasText(proprietes.enTeteIp())) {
            String valeur = request.getHeader(proprietes.enTeteIp());
            if (StringUtils.hasText(valeur)) {
                return valeur.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}

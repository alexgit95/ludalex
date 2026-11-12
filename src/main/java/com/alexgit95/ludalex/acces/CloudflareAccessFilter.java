package com.alexgit95.ludalex.acces;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Exige un JWT Cloudflare Access valide ; aucune identité n'en est conservée.
 * Le chemin {@code /fin/**} est exempté : son token est sa seule autorisation.
 */
public class CloudflareAccessFilter extends OncePerRequestFilter {

    public static final String HEADER_JWT = "Cf-Access-Jwt-Assertion";
    private static final String PRINCIPAL = "cloudflare-access";

    private final JwtDecoder decoder;
    private final RequestMatcher cheminExempte = PathPatternRequestMatcher.withDefaults().matcher("/fin/**");

    public CloudflareAccessFilter(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return cheminExempte.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String jeton = request.getHeader(HEADER_JWT);
        if (!StringUtils.hasText(jeton)) {
            refuser(response, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            decoder.decode(jeton);
        } catch (JwtValidationException e) {
            boolean seulementAudience = e.getErrors().stream()
                    .allMatch(erreur -> CloudflareJwtValidateurs.CODE_AUDIENCE.equals(erreur.getErrorCode()));
            refuser(response, seulementAudience
                    ? HttpServletResponse.SC_FORBIDDEN
                    : HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } catch (JwtException e) {
            refuser(response, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        SecurityContext contexte = SecurityContextHolder.createEmptyContext();
        contexte.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(PRINCIPAL, null, List.of()));
        SecurityContextHolder.setContext(contexte);
        chain.doFilter(request, response);
    }

    private void refuser(HttpServletResponse response, int statut) throws IOException {
        response.setStatus(statut);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(statut == HttpServletResponse.SC_FORBIDDEN ? "Accès refusé" : "Non autorisé");
    }
}

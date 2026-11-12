package com.alexgit95.ludalex.acces;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** En prod, Cloudflare Access est obligatoire quelle que soit la configuration. */
    @Bean
    @Profile("prod")
    SecurityFilterChain chaineProd(HttpSecurity http, JwtDecoder cloudflareJwtDecoder) throws Exception {
        http
                .csrf(Customizer.withDefaults())
                .addFilterBefore(new CloudflareAccessFilter(cloudflareJwtDecoder),
                        UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/fin/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    /** Hors prod (dev local), aucune authentification. */
    @Bean
    @Profile("!prod")
    SecurityFilterChain chaineLibre(HttpSecurity http) throws Exception {
        http
                .csrf(Customizer.withDefaults())
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    @Configuration
    @Profile("prod")
    static class DecodeurCloudflare {

        @Bean
        JwtDecoder cloudflareJwtDecoder(AccesProperties proprietes) {
            proprietes.verifierPourProd();
            NimbusJwtDecoder decodeur = NimbusJwtDecoder.withJwkSetUri(proprietes.jwkSetUri())
                    .jwsAlgorithm(SignatureAlgorithm.RS256)
                    .build();
            decodeur.setJwtValidator(CloudflareJwtValidateurs.creer(proprietes.issuer(), proprietes.audience()));
            return decodeur;
        }
    }
}

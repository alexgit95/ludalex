package com.alexgit95.ludalex.acces;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

/** Validateurs d'un JWT Cloudflare Access : expiration, émetteur et audience. */
public final class CloudflareJwtValidateurs {

    public static final String CODE_AUDIENCE = "invalid_audience";

    private CloudflareJwtValidateurs() {
    }

    public static OAuth2TokenValidator<Jwt> creer(String issuer, String audience) {
        OAuth2TokenValidator<Jwt> parDefaut = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceAttendue = jwt -> jwt.getAudience() != null
                && jwt.getAudience().contains(audience)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                                new OAuth2Error(CODE_AUDIENCE, "Audience non autorisée", null));
        return new DelegatingOAuth2TokenValidator<>(parDefaut, audienceAttendue);
    }
}

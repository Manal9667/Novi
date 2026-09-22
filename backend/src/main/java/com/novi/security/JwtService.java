package com.novi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** The committed dev fallback - fine locally, never acceptable in a real deployment. */
    private static final String DEV_DEFAULT_SECRET = "change-this-development-only-secret-key-please-32bytes+";

    /** HMAC-SHA256 requires a key of at least 256 bits (32 bytes). */
    private static final int MIN_SECRET_BYTES = 32;

    /** Profiles under which the committed development default secret is tolerated. */
    private static final Profiles LOCAL_PROFILES = Profiles.of("dev", "test", "local");

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${novi.security.jwt.secret}") String secret,
            @Value("${novi.security.jwt.expiration-ms}") long expirationMs,
            Environment environment
    ) {
        boolean localProfile = environment.acceptsProfiles(LOCAL_PROFILES);

        if (DEV_DEFAULT_SECRET.equals(secret)) {
            // Fail closed: the built-in secret is public knowledge, so anyone
            // could forge a token for any user (including an admin username).
            // Only tolerate it when a local/dev/test profile is explicitly active.
            if (!localProfile) {
                throw new IllegalStateException(
                        "Refusing to start with the built-in development JWT secret. Set the JWT_SECRET " +
                        "environment variable to a strong, unique value of at least " + MIN_SECRET_BYTES +
                        " bytes, or activate a local profile (e.g. SPRING_PROFILES_ACTIVE=dev) for local development.");
            }
            log.warn("JWT secret is the committed development default; tolerated only because a dev/test/local " +
                    "profile is active. NEVER use this in a real deployment - set JWT_SECRET instead.");
        }

        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT secret must be at least " + MIN_SECRET_BYTES + " bytes (256 bits) for HMAC-SHA256.");
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, String username) {
        try {
            String extracted = extractUsername(token);
            return extracted.equals(username) && !isExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        return resolver.apply(claims);
    }
}

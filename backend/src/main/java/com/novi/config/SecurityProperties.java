package com.novi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Externalized, deployment-tunable security settings under {@code novi.security}.
 *
 * <p>The JWT sub-tree is bound separately by {@code JwtService} via {@code @Value};
 * this class covers the settings that must differ between local dev and a real
 * deployment - who counts as an admin and which browser origins may call the API.
 */
@Configuration
@ConfigurationProperties(prefix = "novi.security")
public class SecurityProperties {

    /**
     * Usernames granted ROLE_ADMIN. Kept as configuration rather than a DB
     * column so the demo needs no schema change or seeded admin row: set
     * {@code ADMIN_USERNAMES} (comma-separated) to elevate accounts.
     */
    private List<String> adminUsernames = new ArrayList<>();

    private Cors cors = new Cors();

    public static class Cors {
        /**
         * Browser origins allowed to call the API. Defaults to any localhost
         * port for dev; override with {@code CORS_ALLOWED_ORIGINS} in prod.
         */
        private List<String> allowedOriginPatterns = new ArrayList<>(List.of("http://localhost:*"));

        public List<String> getAllowedOriginPatterns() { return allowedOriginPatterns; }
        public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
            this.allowedOriginPatterns = allowedOriginPatterns;
        }
    }

    public List<String> getAdminUsernames() { return adminUsernames; }
    public void setAdminUsernames(List<String> adminUsernames) { this.adminUsernames = adminUsernames; }

    public boolean isAdmin(String username) {
        return username != null && adminUsernames.stream().anyMatch(u -> u.equalsIgnoreCase(username));
    }

    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
}

package com.acme.carrental.config;

import java.time.Duration;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private UUID companyId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final Reservation reservation = new Reservation();
    private final Fleet fleet = new Fleet();
    private final Security security = new Security();
    private final Seed seed = new Seed();

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public Reservation getReservation() { return reservation; }
    public Fleet getFleet() { return fleet; }
    public Security getSecurity() { return security; }
    public Seed getSeed() { return seed; }

    public static class Reservation {
        private Duration cleanupInterval = Duration.ofSeconds(30);
        public Duration getCleanupInterval() { return cleanupInterval; }
        public void setCleanupInterval(Duration cleanupInterval) { this.cleanupInterval = cleanupInterval; }
    }

    public static class Fleet {
        private BigDecimal searchRadiusKm = new BigDecimal("25");
        public BigDecimal getSearchRadiusKm() { return searchRadiusKm; }
        public void setSearchRadiusKm(BigDecimal searchRadiusKm) { this.searchRadiusKm = searchRadiusKm; }
    }

    public static class Security {
        private String jwtSecret = "local-development-secret-change-me-32-bytes-minimum";
        private Duration accessTokenTtl = Duration.ofHours(1);
        public String getJwtSecret() { return jwtSecret; }
        public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
        public Duration getAccessTokenTtl() { return accessTokenTtl; }
        public void setAccessTokenTtl(Duration accessTokenTtl) { this.accessTokenTtl = accessTokenTtl; }
    }

    public static class Seed {
        private boolean enabled;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}

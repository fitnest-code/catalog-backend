/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnBean
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.boot.context.event.ApplicationReadyEvent
 *  org.springframework.context.event.EventListener
 *  org.springframework.jdbc.core.JdbcTemplate
 *  org.springframework.stereotype.Component
 */
package az.fitnest.catalog.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(value={JdbcTemplate.class})
@ConditionalOnProperty(prefix="app.warmup", name={"enabled"}, havingValue="true", matchIfMissing=true)
public class StartupWarmupListener {
    private static final Logger log = LoggerFactory.getLogger(StartupWarmupListener.class);
    private final JdbcTemplate jdbcTemplate;

    @EventListener(value={ApplicationReadyEvent.class})
    public void onApplicationReady() {
        log.info("Starting database warmup...");
        this.warmupDatabase();
    }

    private void warmupDatabase() {
        try {
            this.jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.info("Database warmup completed successfully.");
        }
        catch (Exception e) {
            log.warn("Database warmup failed: {}", e.getMessage());
        }
    }

    public StartupWarmupListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}


package az.fitnest.catalog.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(value = {JdbcTemplate.class})
@ConditionalOnProperty(prefix = "app.warmup", name = {"enabled"}, havingValue = "true", matchIfMissing = true)
public class StartupWarmupListener {
    private final JdbcTemplate jdbcTemplate;

    public StartupWarmupListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(value = {ApplicationReadyEvent.class})
    public void onApplicationReady() {
        this.warmupDatabase();
    }

    private void warmupDatabase() {
        try {
            this.jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
        }
    }
}

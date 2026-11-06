package az.fitnest.catalog.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gyms that were already active before the one-time push column existed have a
 * null timestamp. The first boot stamps them so toggling them off and on does
 * not announce them as new. The marker insert makes the stamp run once.
 */
@Component
public class ExistingActiveGymPushMarker {

    private static final Logger log = LoggerFactory.getLogger(ExistingActiveGymPushMarker.class);

    private final JdbcTemplate jdbcTemplate;

    public ExistingActiveGymPushMarker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void stampExistingActiveGyms() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS catalog_one_time_markers (
                        marker VARCHAR(128) PRIMARY KEY
                    )
                    """);
            int inserted = jdbcTemplate.update(
                    "INSERT INTO catalog_one_time_markers (marker) VALUES (?) ON CONFLICT (marker) DO NOTHING",
                    "new_gym_push_existing_active");
            if (inserted == 0) {
                return;
            }
            int stamped = jdbcTemplate.update("""
                    UPDATE gyms
                    SET new_gym_push_sent_at = COALESCE(created_date, CURRENT_TIMESTAMP)
                    WHERE status = 'ACTIVE' AND new_gym_push_sent_at IS NULL
                    """);
            log.info("Stamped {} already-active gyms so they are not announced again", stamped);
        } catch (Exception e) {
            log.warn("Could not stamp existing active gyms for the one-time push: {}", e.getMessage());
        }
    }
}

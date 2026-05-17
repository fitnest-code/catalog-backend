package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Gym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author: nijataghayev
 */

@Repository
public interface GymAnalyticsRepository extends JpaRepository<Gym, Long> {

    // ── KPI: Cari ay aktiv gym sayı və əvvəlki aya nisbət artım faizi ────────
    @Query(value = """
            WITH current_month AS (
                SELECT COUNT(*) AS cnt
                FROM gyms
                WHERE status = 'ACTIVE'
                  AND created_at >= date_trunc('month', CURRENT_DATE)
            ),
            previous_month AS (
                SELECT COUNT(*) AS cnt
                FROM gyms
                WHERE status = 'ACTIVE'
                  AND created_at >= date_trunc('month', CURRENT_DATE - INTERVAL '1 month')
                  AND created_at <  date_trunc('month', CURRENT_DATE)
            )
            SELECT
                (SELECT cnt FROM current_month)  AS total_active_partners,
                CASE
                    WHEN (SELECT cnt FROM previous_month) = 0 THEN 0.0
                    ELSE ROUND(
                        ((SELECT cnt FROM current_month) - (SELECT cnt FROM previous_month))::numeric
                        / (SELECT cnt FROM previous_month) * 100.0,
                    2)
                END AS percentage_change
            """, nativeQuery = true)
    PartnersKpiProjection getActivePartnersKpi();

    interface PartnersKpiProjection {
        Long   getTotalActivePartners();
        Double getPercentageChange();
    }
}

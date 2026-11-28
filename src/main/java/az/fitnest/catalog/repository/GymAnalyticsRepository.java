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

    // Total ACTIVE partners now vs how many were already ACTIVE before this month (MoM growth).
    @Query(value = """
            WITH totals AS (
                SELECT COUNT(*) FILTER (WHERE status = 'ACTIVE') AS total_active,
                       COUNT(*) FILTER (
                           WHERE status = 'ACTIVE'
                             AND created_date < date_trunc('month', CURRENT_DATE)
                       ) AS active_before_month
                FROM gyms
            )
            SELECT
                total_active AS total_active_partners,
                CASE
                    WHEN active_before_month = 0 THEN
                        CASE WHEN total_active > 0 THEN 100.0 ELSE 0.0 END
                    ELSE ROUND(
                        ((total_active - active_before_month)::numeric
                            / active_before_month) * 100.0,
                        2)
                END AS percentage_change
            FROM totals
            """, nativeQuery = true)
    PartnersKpiProjection getActivePartnersKpi();

    interface PartnersKpiProjection {
        Long   getTotalActivePartners();
        Double getPercentageChange();
    }
}

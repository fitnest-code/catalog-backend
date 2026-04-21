package az.fitnest.catalog.repository;

import az.fitnest.catalog.dto.admin.AdminPanelGymListDto;
import az.fitnest.catalog.model.entity.GymAdminPanel;
import az.fitnest.catalog.model.enums.GymStatus;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GymAdminPanelRepository extends JpaRepository<GymAdminPanel, Long> {

    @Query("""
                SELECT DISTINCT new az.fitnest.catalog.model.dto.AdminGymListDto(
                    g.id,
                    g.name,
                    g.address.city,
                    g.address.district,
                    ga.name,
                    ga.surname,
                    g.status
                )
                FROM GymAdminPanel g
                LEFT JOIN GymAdmin ga ON ga.gym = g
                WHERE (:status IS NULL OR g.status = :status)
                  AND (:cityName IS NULL OR g.address.city = :cityName)
                  AND (:districtName IS NULL OR g.address.district = :districtName)
                  AND (:search IS NULL OR
                       LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       LOWER(g.address.city) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       LOWER(g.address.district) LIKE LOWER(CONCAT('%', :search, '%')) OR
                       LOWER(CONCAT(ga.name, ' ', ga.surname)) LIKE LOWER(CONCAT('%', :search, '%'))
                  )
            """)
    Page<AdminPanelGymListDto> findAllForAdmin(
            @Param("search") String search,
            @Param("status") GymStatus status,
            @Param("cityName") String cityName,
            @Param("districtName") String districtName,
            Pageable pageable
    );

    @Query("""
                SELECT DISTINCT g.address.city
                FROM GymAdminPanel g
                WHERE g.address.city IS NOT NULL
                ORDER BY g.address.city ASC
            """)
    List<String> findCities();

    @Query("""
                SELECT DISTINCT g.address.district
                FROM GymAdminPanel g
                WHERE g.address.city = :city
                  AND g.address.district IS NOT NULL
                ORDER BY g.address.district ASC
            """)
    List<String> findDistrictsByCity(@Param("city") String city);
}


package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Gym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GymRepository
        extends JpaRepository<Gym, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Gym> {
    public List<Gym> findByAddressLatitudeBetweenAndAddressLongitudeBetween(Double var1, Double var2, Double var3, Double var4);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM gyms WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng " +
            "ORDER BY (6371 * acos(least(1, cos(radians(:userLat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:userLng)) + sin(radians(:userLat)) * sin(radians(latitude))))) ASC",
            countQuery = "SELECT count(*) FROM gyms WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng", nativeQuery = true)
    public org.springframework.data.domain.Page<Gym> findClosestGyms(@org.springframework.data.repository.query.Param("minLat") Double minLat, @org.springframework.data.repository.query.Param("maxLat") Double maxLat, @org.springframework.data.repository.query.Param("minLng") Double minLng, @org.springframework.data.repository.query.Param("maxLng") Double maxLng, @org.springframework.data.repository.query.Param("userLat") Double userLat, @org.springframework.data.repository.query.Param("userLng") Double userLng, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM gyms " +
            "ORDER BY (6371 * acos(least(1, cos(radians(:userLat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:userLng)) + sin(radians(:userLat)) * sin(radians(latitude))))) ASC",
            countQuery = "SELECT count(*) FROM gyms", nativeQuery = true)
    public org.springframework.data.domain.Page<Gym> findAllClosest(@org.springframework.data.repository.query.Param("userLat") Double userLat, @org.springframework.data.repository.query.Param("userLng") Double userLng, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM gyms WHERE (LOWER(name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(description) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(address_text) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))) " +
            "ORDER BY (6371 * acos(least(1, cos(radians(:userLat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:userLng)) + sin(radians(:userLat)) * sin(radians(latitude))))) ASC",
            countQuery = "SELECT count(*) FROM gyms WHERE (LOWER(name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(description) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(address_text) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))", nativeQuery = true)
    public org.springframework.data.domain.Page<Gym> searchClosest(@org.springframework.data.repository.query.Param("q") String q, @org.springframework.data.repository.query.Param("userLat") Double userLat, @org.springframework.data.repository.query.Param("userLng") Double userLng, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT w FROM Gym g JOIN g.generalWorkHours w WHERE g.id = :gymId")
    public List<az.fitnest.catalog.model.entity.GymWorkHour> findGeneralWorkHoursByGymId(@org.springframework.data.repository.query.Param("gymId") Long gymId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"address", "category", "subscriptions", "subscriptions.supportedServices", "rooms", "rooms.images", "generalWorkHours", "workHoursWoman", "workHoursMan", "restDays"})
    public Optional<Gym> findWithDetailsById(Long id);

    @org.springframework.data.jpa.repository.Query("SELECT g FROM Gym g WHERE (LOWER(g.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR LOWER(g.address.addressText) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))")
    public org.springframework.data.domain.Page<Gym> findByNameOrDescriptionContainingIgnoreCase(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT g FROM Gym g WHERE g.category.id = :categoryId")
    public org.springframework.data.domain.Page<Gym> findByCategory(@org.springframework.data.repository.query.Param("categoryId") Long categoryId, org.springframework.data.domain.Pageable pageable);

    public boolean existsByCategoryId(Long categoryId);

    @org.springframework.data.jpa.repository.Query("SELECT g FROM Gym g WHERE (LOWER(g.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR LOWER(g.address.addressText) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))) AND g.category.id = :categoryId")
    public org.springframework.data.domain.Page<Gym> findByNameOrDescriptionContainingIgnoreCaseAndCategory(@org.springframework.data.repository.query.Param("q") String q, @org.springframework.data.repository.query.Param("categoryId") Long categoryId, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = "SELECT g.* FROM gyms g WHERE g.category_id = :categoryId " +
            "ORDER BY (6371 * acos(least(1, cos(radians(:userLat)) * cos(radians(g.latitude)) * cos(radians(g.longitude) - radians(:userLng)) + sin(radians(:userLat)) * sin(radians(g.latitude))))) ASC",
            countQuery = "SELECT count(*) FROM gyms g WHERE g.category_id = :categoryId", nativeQuery = true)
    public org.springframework.data.domain.Page<Gym> findByCategoryClosest(@org.springframework.data.repository.query.Param("categoryId") Long categoryId, @org.springframework.data.repository.query.Param("userLat") Double userLat, @org.springframework.data.repository.query.Param("userLng") Double userLng, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = "SELECT g.* FROM gyms g WHERE (LOWER(g.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(g.address_text) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))) " +
            "AND g.category_id = :categoryId " +
            "ORDER BY (6371 * acos(least(1, cos(radians(:userLat)) * cos(radians(g.latitude)) * cos(radians(g.longitude) - radians(:userLng)) + sin(radians(:userLat)) * sin(radians(g.latitude))))) ASC",
            countQuery = "SELECT count(*) FROM gyms g WHERE (LOWER(g.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(g.address_text) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))) AND g.category_id = :categoryId", nativeQuery = true)
    public org.springframework.data.domain.Page<Gym> searchClosestWithCategory(@org.springframework.data.repository.query.Param("q") String q, @org.springframework.data.repository.query.Param("categoryId") Long categoryId, @org.springframework.data.repository.query.Param("userLat") Double userLat, @org.springframework.data.repository.query.Param("userLng") Double userLng, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT g FROM Gym g WHERE " +
            "LOWER(g.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR " +
            "LOWER(g.address.addressText) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR " +
            "LOWER(g.category.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))")
    org.springframework.data.domain.Page<Gym> searchByNameAddressCategory(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);


    @org.springframework.data.jpa.repository.Query(value = "SELECT cover_image_url FROM gyms WHERE cover_image_url IS NOT NULL " +
            "UNION ALL " +
            "SELECT qr_code_url FROM gyms WHERE qr_code_url IS NOT NULL " +
            "UNION ALL " +
            "SELECT profile_image_url FROM trainers WHERE profile_image_url IS NOT NULL " +
            "UNION ALL " +
            "SELECT picture_url FROM room_images WHERE picture_url IS NOT NULL " +
            "UNION ALL " +
            "SELECT url FROM gym_images WHERE url IS NOT NULL", nativeQuery = true)
    List<String> findAllGymRelatedFileUrls();

    @org.springframework.data.jpa.repository.Query("SELECT g.qrCodeUrl FROM Gym g WHERE g.id = :gymId")
    String findQrCodeUrlById(@org.springframework.data.repository.query.Param("gymId") Long gymId);


    @org.springframework.data.jpa.repository.Query("SELECT w FROM Gym g JOIN g.generalWorkHours w WHERE g.id IN :gymIds")
    public List<az.fitnest.catalog.model.entity.GymWorkHour> findAllGeneralWorkHoursByGymIds(@org.springframework.data.repository.query.Param("gymIds") List<Long> gymIds);

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT g FROM Gym g " +
                    "WHERE (:hasSubscriptionFilter = false OR EXISTS (SELECT s FROM g.subscriptions s WHERE s.packageId IN :subscriptionIds))",
            countQuery = "SELECT count(g) FROM Gym g WHERE (:hasSubscriptionFilter = false OR EXISTS (SELECT s FROM g.subscriptions s WHERE s.packageId IN :subscriptionIds))"
    )
    org.springframework.data.domain.Page<Gym> findAllWithSpecificationFallback(
            @org.springframework.data.repository.query.Param("hasSubscriptionFilter") boolean hasSubscriptionFilter,
            @org.springframework.data.repository.query.Param("subscriptionIds") List<Long> subscriptionIds,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"category", "subscriptions"})
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT g FROM Gym g " +
                    "WHERE (:hasSubscriptionFilter = false OR EXISTS (SELECT s FROM g.subscriptions s WHERE s.packageId IN :subscriptionIds)) " +
                    "AND (:categoryId IS NULL OR g.category.id = :categoryId) " +
                    "AND (:q IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')) OR LOWER(g.address.addressText) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))"
    )
    org.springframework.data.domain.Page<Gym> findAllGymsWithFilters(
            @org.springframework.data.repository.query.Param("q") String q,
            @org.springframework.data.repository.query.Param("categoryId") Long categoryId,
            @org.springframework.data.repository.query.Param("hasSubscriptionFilter") boolean hasSubscriptionFilter,
            @org.springframework.data.repository.query.Param("subscriptionIds") List<Long> subscriptionIds,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT DISTINCT g.* FROM gyms g " +
                    "LEFT JOIN gym_subscriptions gs ON g.id = gs.gym_id " +
                    "WHERE (:hasSubscriptionFilter = false OR gs.package_id IN :subscriptionIds) " +
                    "AND (:categoryId IS NULL OR g.category_id = :categoryId) " +
                    "AND (:q IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(g.address_text) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))) " +
                    "ORDER BY (6371 * acos(least(1, cos(radians(:userLat)) * cos(radians(g.latitude)) * cos(radians(g.longitude) - radians(:userLng)) + sin(radians(:userLat)) * sin(radians(g.latitude))))) ASC",
            countQuery = "SELECT count(DISTINCT g.id) FROM gyms g " +
                    "LEFT JOIN gym_subscriptions gs ON g.id = gs.gym_id " +
                    "WHERE (:hasSubscriptionFilter = false OR gs.package_id IN :subscriptionIds) " +
                    "AND (:categoryId IS NULL OR g.category_id = :categoryId) " +
                    "AND (:q IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(g.address_text) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))",
            nativeQuery = true
    )
    org.springframework.data.domain.Page<Gym> findAllClosestWithFiltersNative(
            @org.springframework.data.repository.query.Param("q") String q,
            @org.springframework.data.repository.query.Param("categoryId") Long categoryId,
            @org.springframework.data.repository.query.Param("hasSubscriptionFilter") boolean hasSubscriptionFilter,
            @org.springframework.data.repository.query.Param("subscriptionIds") List<Long> subscriptionIds,
            @org.springframework.data.repository.query.Param("userLat") Double userLat,
            @org.springframework.data.repository.query.Param("userLng") Double userLng,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT g.id FROM gyms g " +
                    "LEFT JOIN gym_subscriptions gs ON g.id = gs.gym_id " +
                    "WHERE (:hasSubscriptionFilter = false OR gs.package_id IN :subscriptionIds) " +
                    "AND (:categoryId IS NULL OR g.category_id = :categoryId) " +
                    "AND (:q IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(g.address_text) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))) " +
                    "GROUP BY g.id, g.latitude, g.longitude " +
                    "ORDER BY (6371 * acos(least(1, cos(radians(:userLat)) * cos(radians(g.latitude)) * cos(radians(g.longitude) - radians(:userLng)) + sin(radians(:userLat)) * sin(radians(g.latitude))))) ASC",
            countQuery = "SELECT count(DISTINCT g.id) FROM gyms g " +
                    "LEFT JOIN gym_subscriptions gs ON g.id = gs.gym_id " +
                    "WHERE (:hasSubscriptionFilter = false OR gs.package_id IN :subscriptionIds) " +
                    "AND (:categoryId IS NULL OR g.category_id = :categoryId) " +
                    "AND (:q IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')) OR LOWER(g.address_text) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))",
            nativeQuery = true
    )
    org.springframework.data.domain.Page<Long> findAllClosestWithFiltersNativeIds(
            @org.springframework.data.repository.query.Param("q") String q,
            @org.springframework.data.repository.query.Param("categoryId") Long categoryId,
            @org.springframework.data.repository.query.Param("hasSubscriptionFilter") boolean hasSubscriptionFilter,
            @org.springframework.data.repository.query.Param("subscriptionIds") List<Long> subscriptionIds,
            @org.springframework.data.repository.query.Param("userLat") Double userLat,
            @org.springframework.data.repository.query.Param("userLng") Double userLng,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"category", "subscriptions"})
    public List<Gym> findWithListDetailsByIdIn(List<Long> ids);
}

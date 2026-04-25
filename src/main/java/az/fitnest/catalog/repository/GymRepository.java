
package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Gym;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface GymRepository
        extends JpaRepository<Gym, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Gym> {
    public List<Gym> findByAddressLatitudeBetweenAndAddressLongitudeBetween(Double var1, Double var2, Double var3, Double var4);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM gyms WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng " +
            "ORDER BY (6371 * acos(least(1, cos(radians(:userLat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:userLng)) + sin(radians(:userLat)) * sin(radians(latitude))))) ASC", nativeQuery = true)
    public org.springframework.data.domain.Page<Gym> findClosestGyms(@org.springframework.data.repository.query.Param("minLat") Double minLat, @org.springframework.data.repository.query.Param("maxLat") Double maxLat, @org.springframework.data.repository.query.Param("minLng") Double minLng, @org.springframework.data.repository.query.Param("maxLng") Double maxLng, @org.springframework.data.repository.query.Param("userLat") Double userLat, @org.springframework.data.repository.query.Param("userLng") Double userLng, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM gyms " +
            "ORDER BY (6371 * acos(least(1, cos(radians(:userLat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:userLng)) + sin(radians(:userLat)) * sin(radians(latitude))))) ASC", nativeQuery = true)
    public org.springframework.data.domain.Page<Gym> findAllClosest(@org.springframework.data.repository.query.Param("userLat") Double userLat, @org.springframework.data.repository.query.Param("userLng") Double userLng, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM gyms WHERE (LOWER(name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(description) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(address_text) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY (6371 * acos(least(1, cos(radians(:userLat)) * cos(radians(latitude)) * cos(radians(longitude) - radians(:userLng)) + sin(radians(:userLat)) * sin(radians(latitude))))) ASC", nativeQuery = true)
    public org.springframework.data.domain.Page<Gym> searchClosest(@org.springframework.data.repository.query.Param("q") String q, @org.springframework.data.repository.query.Param("userLat") Double userLat, @org.springframework.data.repository.query.Param("userLng") Double userLng, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT w FROM Gym g JOIN g.generalWorkHours w WHERE g.id = :gymId")
    public List<az.fitnest.catalog.model.entity.GymWorkHour> findGeneralWorkHoursByGymId(@org.springframework.data.repository.query.Param("gymId") Long gymId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"address", "categories", "subscriptions", "subscriptions.benefits", "rooms", "rooms.images", "generalWorkHours", "workHoursWoman", "workHoursMan"})
    public Optional<Gym> findWithDetailsById(Long id);

    @org.springframework.data.jpa.repository.Query("SELECT g FROM Gym g WHERE (LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(g.address.addressText) LIKE LOWER(CONCAT('%', :q, '%')))")
    public org.springframework.data.domain.Page<Gym> findByNameOrDescriptionContainingIgnoreCase(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT g FROM Gym g JOIN g.categories c WHERE c.id = :categoryId")
    public org.springframework.data.domain.Page<Gym> findByCategory(@org.springframework.data.repository.query.Param("categoryId") Long categoryId, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT g FROM Gym g JOIN g.categories c WHERE (LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(g.address.addressText) LIKE LOWER(CONCAT('%', :q, '%'))) AND c.id = :categoryId")
    public org.springframework.data.domain.Page<Gym> findByNameOrDescriptionContainingIgnoreCaseAndCategory(@org.springframework.data.repository.query.Param("q") String q, @org.springframework.data.repository.query.Param("categoryId") Long categoryId, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = "SELECT g.* FROM gyms g WHERE EXISTS (SELECT 1 FROM gym_categories gc WHERE gc.gym_id = g.id AND gc.category_id = :categoryId) " +
            "ORDER BY (6371 * acos(least(1, cos(radians(:userLat)) * cos(radians(g.latitude)) * cos(radians(g.longitude) - radians(:userLng)) + sin(radians(:userLat)) * sin(radians(g.latitude))))) ASC", nativeQuery = true)
    public org.springframework.data.domain.Page<Gym> findByCategoryClosest(@org.springframework.data.repository.query.Param("categoryId") Long categoryId, @org.springframework.data.repository.query.Param("userLat") Double userLat, @org.springframework.data.repository.query.Param("userLng") Double userLng, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query(value = "SELECT g.* FROM gyms g WHERE (LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(g.address_text) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND EXISTS (SELECT 1 FROM gym_categories gc WHERE gc.gym_id = g.id AND gc.category_id = :categoryId) " +
            "ORDER BY (6371 * acos(least(1, cos(radians(:userLat)) * cos(radians(g.latitude)) * cos(radians(g.longitude) - radians(:userLng)) + sin(radians(:userLat)) * sin(radians(g.latitude))))) ASC", nativeQuery = true)
    public org.springframework.data.domain.Page<Gym> searchClosestWithCategory(@org.springframework.data.repository.query.Param("q") String q, @org.springframework.data.repository.query.Param("categoryId") Long categoryId, @org.springframework.data.repository.query.Param("userLat") Double userLat, @org.springframework.data.repository.query.Param("userLng") Double userLng, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT DISTINCT g FROM Gym g LEFT JOIN g.categories c WHERE " +
           "LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(g.address.addressText) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    org.springframework.data.domain.Page<Gym> searchByNameAddressCategory(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM gym_categories", nativeQuery = true)
    void truncateGymCategories();

    @org.springframework.data.jpa.repository.Query("SELECT g.qrCodeUrl FROM Gym g WHERE g.id = :gymId")
    String findQrCodeUrlById(@org.springframework.data.repository.query.Param("gymId") Long gymId);
}

/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.stereotype.Repository
 */
package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Gym;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GymRepository
extends JpaRepository<Gym, Long> {
    public List<Gym> findByAddressLatitudeBetweenAndAddressLongitudeBetween(Double var1, Double var2, Double var3, Double var4);

    @org.springframework.data.jpa.repository.Query("SELECT g FROM Gym g WHERE g.address.latitude BETWEEN :minLat AND :maxLat AND g.address.longitude BETWEEN :minLng AND :maxLng ORDER BY ((g.address.latitude - :userLat) * (g.address.latitude - :userLat) + (g.address.longitude - :userLng) * (g.address.longitude - :userLng)) ASC")
    public org.springframework.data.domain.Page<Gym> findClosestGyms(@org.springframework.data.repository.query.Param("minLat") Double minLat, @org.springframework.data.repository.query.Param("maxLat") Double maxLat, @org.springframework.data.repository.query.Param("minLng") Double minLng, @org.springframework.data.repository.query.Param("maxLng") Double maxLng, @org.springframework.data.repository.query.Param("userLat") Double userLat, @org.springframework.data.repository.query.Param("userLng") Double userLng, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT g FROM Gym g WHERE (LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(g.description) LIKE LOWER(CONCAT('%', :q, '%'))) AND g.address.latitude BETWEEN :minLat AND :maxLat AND g.address.longitude BETWEEN :minLng AND :maxLng ORDER BY ((g.address.latitude - :userLat) * (g.address.latitude - :userLat) + (g.address.longitude - :userLng) * (g.address.longitude - :userLng)) ASC")
    public org.springframework.data.domain.Page<Gym> findClosestGymsWithQuery(@org.springframework.data.repository.query.Param("q") String q, @org.springframework.data.repository.query.Param("minLat") Double minLat, @org.springframework.data.repository.query.Param("maxLat") Double maxLat, @org.springframework.data.repository.query.Param("minLng") Double minLng, @org.springframework.data.repository.query.Param("maxLng") Double maxLng, @org.springframework.data.repository.query.Param("userLat") Double userLat, @org.springframework.data.repository.query.Param("userLng") Double userLng, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT w FROM Gym g JOIN g.workHours w WHERE g.id = :gymId")
    public List<az.fitnest.catalog.model.entity.GymWorkHour> findWorkHoursByGymId(@org.springframework.data.repository.query.Param("gymId") Long gymId);
}


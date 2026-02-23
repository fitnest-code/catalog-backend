/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository
extends JpaRepository<Review, Long> {
    @Query(value="SELECT r FROM Gym g JOIN g.reviews r WHERE g.id = :gymId")
    public Page<Review> findByGymId(@Param(value="gymId") Long var1, Pageable var2);
}


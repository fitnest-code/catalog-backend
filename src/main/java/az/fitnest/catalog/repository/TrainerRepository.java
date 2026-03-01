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

import az.fitnest.catalog.model.entity.Trainer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TrainerRepository
        extends JpaRepository<Trainer, Long> {
    @Query(value = "SELECT t FROM Gym g JOIN g.trainers t WHERE g.id = :gymId")
    public Page<Trainer> findByGymId(@Param(value = "gymId") Long var1, Pageable var2);

    @Modifying
    @Transactional
    @Query("UPDATE Trainer t SET t.profession = NULL")
    void clearAllProfessions();
}


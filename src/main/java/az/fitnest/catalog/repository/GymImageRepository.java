/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymImage;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymImageRepository
extends JpaRepository<GymImage, Long> {
    public List<GymImage> findByGym(Gym var1);

    public List<GymImage> findByGymId(Long var1);

    public Optional<GymImage> findFirstByGymIdAndImageName(Long gymId, String imageName);
}


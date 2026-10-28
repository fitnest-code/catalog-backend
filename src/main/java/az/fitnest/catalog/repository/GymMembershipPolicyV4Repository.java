package az.fitnest.catalog.repository;

import az.fitnest.catalog.model.entity.GymMembershipPolicyV4Entity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymMembershipPolicyV4Repository
    extends JpaRepository<GymMembershipPolicyV4Entity, Long> {}

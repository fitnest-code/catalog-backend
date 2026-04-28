package az.fitnest.catalog.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "gym_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GymSubscription extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @Column(name = "package_id")
    private Long packageId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "gym_subscription_services",
        joinColumns = @JoinColumn(name = "gym_subscription_id"),
        inverseJoinColumns = @JoinColumn(name = "supported_service_id")
    )
    private Set<SupportedService> supportedServices = new HashSet<>();
}

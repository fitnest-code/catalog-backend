package az.fitnest.catalog.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_panel_gym_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPanelGymSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private GymAdminPanel gym;

    @Column(name = "subscription_type_id", nullable = false)
    private Long subscriptionTypeId;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;
}

package az.fitnest.catalog.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gym_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GymSubscription extends BaseAuditableEntity {

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "gym_subscription_benefits", joinColumns = @JoinColumn(name = "gym_subscription_id"))
    private List<GymSubscriptionBenefit> benefits = new ArrayList<>();

}

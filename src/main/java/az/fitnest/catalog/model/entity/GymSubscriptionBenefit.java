package az.fitnest.catalog.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GymSubscriptionBenefit {

    @Column(name = "benefit", nullable = false)
    private String benefit;

    @Column(name = "benefit_logo")
    private String benefitLogo;

}

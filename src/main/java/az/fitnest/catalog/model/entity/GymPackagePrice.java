package az.fitnest.catalog.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Versioned home-gym pricing for a package inside a gym (spec R15/R17).
 * A new row is written for the first day of the next month instead of updating
 * the current one, so historical visits keep their snapshot.
 */
@Entity
@Table(name = "gym_package_price")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GymPackagePrice extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "included")
    @Builder.Default
    private Boolean included = true;

    /**
     * Optional package-level home-gym switch. When null the gym master switch applies.
     */
    @Column(name = "home_eligible")
    private Boolean homeEligible;

    @Column(name = "single_price")
    private Double singlePrice;

    @Column(name = "unlimited_price")
    private Double unlimitedPrice;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
}

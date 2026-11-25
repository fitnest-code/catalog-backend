package az.fitnest.catalog.model.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

/**
 * Entry / monthly price for a gym category (main or sub). Separate from
 * {@link GymPackagePrice} so home-gym V4 sync keeps its unique key unchanged.
 */
@Entity
@Table(name = "gym_category_package_price")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GymCategoryPackagePrice extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "single_price")
    private Double singlePrice;

    @Column(name = "unlimited_price")
    private Double unlimitedPrice;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;
}

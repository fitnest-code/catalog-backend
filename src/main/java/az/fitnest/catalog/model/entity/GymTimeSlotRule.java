package az.fitnest.catalog.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Optional time-window rule for a gym (spec R14/R15). Rules are versioned by
 * {@code effectiveFrom}/{@code effectiveTo}; the most specific matching rule for
 * the Baku weekday and time wins.
 */
@Entity
@Table(name = "gym_time_slot_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GymTimeSlotRule extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    /**
     * Comma separated day names, e.g. {@code MONDAY,TUESDAY}. Empty/null means every day.
     */
    @Column(name = "weekdays", length = 128)
    private String weekdays;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "gym_time_slot_rule_packages", joinColumns = @JoinColumn(name = "rule_id"))
    @Column(name = "package_id")
    @Builder.Default
    @BatchSize(size = 50)
    private Set<Long> allowedPackageIds = new HashSet<>();

    /**
     * Package that provides the price for visits inside this slot (R15).
     */
    @Column(name = "price_package_id")
    private Long pricePackageId;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    public boolean isEffectiveOn(LocalDate date) {
        if (effectiveFrom != null && date.isBefore(effectiveFrom)) {
            return false;
        }
        return effectiveTo == null || !date.isAfter(effectiveTo);
    }
}

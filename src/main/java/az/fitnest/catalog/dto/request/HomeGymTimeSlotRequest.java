package az.fitnest.catalog.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Optional time-window rule for home-gym entries (spec R14/R15).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeGymTimeSlotRequest {

    /**
     * Day names such as {@code MONDAY}. Empty/null means every day.
     */
    private List<String> weekdays;

    /**
     * Window start in {@code HH:mm}. Both start and end must be set, or both null for all day.
     */
    private String startTime;

    /**
     * Window end in {@code HH:mm}. An end before start represents an overnight window.
     */
    private String endTime;

    /**
     * Packages allowed to enter inside this window. Empty/null means every package.
     */
    private List<Long> allowedPackageIds;

    /**
     * Package whose price is used for visits inside this window. Falls back to the
     * subscriber package when absent.
     */
    private Long pricePackageId;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}

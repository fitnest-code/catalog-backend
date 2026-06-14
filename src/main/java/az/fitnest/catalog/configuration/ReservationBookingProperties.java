package az.fitnest.catalog.configuration;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author: nijataghayev
 */

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "reservation.booking")
public class ReservationBookingProperties {

    @Min(0)
    private int minHoursBeforeStart = 2;
}

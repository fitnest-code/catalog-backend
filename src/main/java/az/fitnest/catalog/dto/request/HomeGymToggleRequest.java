package az.fitnest.catalog.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * R18 admin switch for "can be selected as home gym".
 *
 * <p>When {@code enabled} changes from true to false, the localized pop-up
 * title/body are published to {@code home-gym-events} so order-backend can
 * schedule the end of existing selections and notify subscribers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeGymToggleRequest {

    private Boolean enabled;

    /**
     * Localized message shown in the admin pop-up, keyed by {@code az}, {@code en}, {@code ru}.
     */
    private Map<String, String> title;

    /**
     * Localized message body, keyed by {@code az}, {@code en}, {@code ru}.
     */
    private Map<String, String> body;

    /**
     * Optional subscriber user ids shown in the admin pop-up. Order-backend derives
     * the active subscribers when this list is empty.
     */
    private List<Long> recipients;
}

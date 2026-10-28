package az.fitnest.catalog.service.homegym;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Publishes package-removal events for the home-gym feature (F-04).
 *
 * <p>When a package is removed from a gym, order-backend must apply the R18 flow to the
 * subscribers that lose access: the existing home gym keeps working until the end of the
 * current subscription month and then becomes ENDED. The event only carries the gym and
 * the removed package ids; order re-checks each subscriber's package hierarchy through
 * gRPC so inherited access is evaluated correctly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HomeGymPackageEventPublisher {

    public static final String HOME_GYM_EVENTS_TOPIC = "home-gym-events";
    public static final String HOME_GYM_PACKAGE_REMOVED = "HOME_GYM_PACKAGE_REMOVED";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void packageRemoved(Long gymId, String gymName, Collection<Long> packageIds) {
        if (gymId == null || packageIds == null || packageIds.isEmpty()) {
            return;
        }
        List<Long> distinct = new ArrayList<>(new LinkedHashSet<>(packageIds));
        distinct.removeIf(java.util.Objects::isNull);
        if (distinct.isEmpty()) {
            return;
        }
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", HOME_GYM_PACKAGE_REMOVED);
        event.put("gymId", gymId);
        event.put("gymName", gymName);
        event.put("packageIds", distinct);
        event.put("occurredAt", OffsetDateTime.now(HomeGymRuleService.BAKU).toString());

        Runnable send = () -> {
            try {
                kafkaTemplate.send(HOME_GYM_EVENTS_TOPIC, String.valueOf(gymId), event);
            } catch (Exception e) {
                log.error("Could not publish {} for gym {} packages {}",
                        HOME_GYM_PACKAGE_REMOVED, gymId, distinct, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}

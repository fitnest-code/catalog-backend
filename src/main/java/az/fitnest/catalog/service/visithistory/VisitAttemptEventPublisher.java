package az.fitnest.catalog.service.visithistory;

import az.fitnest.catalog.service.homegym.HomeGymRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes rejected QR attempts that catalog evaluates before calling order
 * (frozen/expired subscription, package exclusion, R14 time-slot rule).
 *
 * <p>Order never sees those scans, so it cannot record them itself. The event is sent
 * after the scan transaction commits: a rolled-back scan must not appear in the
 * subscriber's history. The topic is shared with the home-gym events but the
 * {@code eventType} discriminates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VisitAttemptEventPublisher {

    public static final String TOPIC = "visit-attempt-events";
    public static final String VISIT_ATTEMPT_REJECTED = "VISIT_ATTEMPT_REJECTED";

    public static final String SUBSCRIPTION_FROZEN = "SUBSCRIPTION_FROZEN";
    public static final String SUBSCRIPTION_EXPIRED = "SUBSCRIPTION_EXPIRED";
    public static final String GYM_NOT_IN_PACKAGE = "GYM_NOT_IN_PACKAGE";
    public static final String TIER_NOT_ALLOWED_AT_THIS_TIME = "TIER_NOT_ALLOWED_AT_THIS_TIME";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void rejected(Long userId,
                         Long gymId,
                         String gymName,
                         String packageName,
                         String qrScanId,
                         String rejectCode,
                         LocalDateTime scannedAt) {
        if (userId == null || gymId == null || rejectCode == null) {
            return;
        }
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", VISIT_ATTEMPT_REJECTED);
        event.put("userId", userId);
        event.put("gymId", gymId);
        event.put("gymName", gymName);
        event.put("package", packageName);
        event.put("qrScanId", qrScanId);
        event.put("rejectCode", rejectCode);
        event.put("scannedAt", (scannedAt != null ? scannedAt : LocalDateTime.now(HomeGymRuleService.BAKU))
                .atZone(HomeGymRuleService.BAKU)
                .toOffsetDateTime()
                .toString());

        Runnable send = () -> {
            try {
                kafkaTemplate.send(TOPIC, String.valueOf(userId), event);
            } catch (Exception e) {
                log.error("Could not publish {} for user {} at gym {} ({})",
                        VISIT_ATTEMPT_REJECTED, userId, gymId, rejectCode, e);
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

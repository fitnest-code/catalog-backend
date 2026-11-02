package az.fitnest.catalog.service.visithistory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Contract of {@code VISIT_ATTEMPT_REJECTED}: order-backend reads exactly these fields,
 * and a scan that is rolled back must not leave a history row.
 */
@ExtendWith(MockitoExtension.class)
class VisitAttemptEventPublisherTest {

    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishesRejectionWithTheFieldsOrderExpects() {
        VisitAttemptEventPublisher publisher = new VisitAttemptEventPublisher(kafkaTemplate);

        publisher.rejected(7L, 10L, "Health Fit", "Bronze", "scan-1",
                VisitAttemptEventPublisher.TIER_NOT_ALLOWED_AT_THIS_TIME,
                LocalDateTime.of(2026, 11, 2, 17, 20));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaTemplate).send(eq("visit-attempt-events"), eq("7"), captor.capture());
        Map<String, Object> event = captor.getValue();
        assertThat(event)
                .containsEntry("eventType", "VISIT_ATTEMPT_REJECTED")
                .containsEntry("userId", 7L)
                .containsEntry("gymId", 10L)
                .containsEntry("gymName", "Health Fit")
                .containsEntry("package", "Bronze")
                .containsEntry("qrScanId", "scan-1")
                .containsEntry("rejectCode", "TIER_NOT_ALLOWED_AT_THIS_TIME")
                .containsEntry("scannedAt", "2026-11-02T17:20+04:00");
    }

    @Test
    void publishesOnlyAfterTheScanTransactionCommits() {
        VisitAttemptEventPublisher publisher = new VisitAttemptEventPublisher(kafkaTemplate);
        TransactionSynchronizationManager.initSynchronization();

        publisher.rejected(7L, 10L, "Health Fit", "Bronze", "scan-1",
                VisitAttemptEventPublisher.GYM_NOT_IN_PACKAGE,
                LocalDateTime.of(2026, 11, 2, 17, 20));

        verifyNoInteractions(kafkaTemplate);

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        verify(kafkaTemplate).send(eq("visit-attempt-events"), eq("7"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ignoresIncompleteRejections() {
        VisitAttemptEventPublisher publisher = new VisitAttemptEventPublisher(kafkaTemplate);

        publisher.rejected(null, 10L, "Health Fit", "Bronze", "scan-1",
                VisitAttemptEventPublisher.SUBSCRIPTION_FROZEN, null);
        publisher.rejected(7L, 10L, "Health Fit", "Bronze", "scan-1", null, null);

        verifyNoInteractions(kafkaTemplate);
    }
}

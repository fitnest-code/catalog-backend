package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.NotificationsServiceGrpcClient;
import az.fitnest.catalog.service.GymActivationNotifier;
import az.fitnest.catalog.service.NewGymPushClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class GymActivationNotifierImpl implements GymActivationNotifier {

    private final NotificationsServiceGrpcClient notificationsServiceClient;
    private final NewGymPushClaimService newGymPushClaimService;

    @Override
    public void notifyNewActiveGym(Long gymId, String gymName) {
        if (gymId == null) {
            return;
        }
        LocalDateTime claimedAt = newGymPushClaimService.claim(gymId);
        if (claimedAt == null) {
            return;
        }
        final String name = gymName != null ? gymName : "";
        Runnable send = () -> {
            boolean sent = notificationsServiceClient.notifyNewGym(gymId, name);
            if (!sent) {
                newGymPushClaimService.release(gymId, claimedAt);
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(send);
                }
            });
        } else {
            CompletableFuture.runAsync(send);
        }
    }
}

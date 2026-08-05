package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.NotificationsServiceGrpcClient;
import az.fitnest.catalog.service.GymActivationNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class GymActivationNotifierImpl implements GymActivationNotifier {

    private final NotificationsServiceGrpcClient notificationsServiceClient;

    @Override
    public void notifyNewActiveGym(Long gymId, String gymName) {
        if (gymId == null) {
            return;
        }
        final Long id = gymId;
        final String name = gymName != null ? gymName : "";

        Runnable send = () -> {
            try {
                notificationsServiceClient.notifyNewGym(id, name);
            } catch (Exception ignored) {
                // Gym persistence must not fail because of notification delivery
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

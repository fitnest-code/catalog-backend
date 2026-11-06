package az.fitnest.catalog.service.impl;

import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.fitnest.catalog.client.NotificationsServiceGrpcClient;
import az.fitnest.catalog.service.NewGymPushClaimService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class GymActivationNotifierImplTest {

    @Test
    void secondActivationDoesNotSend() {
        NewGymPushClaimService claim = org.mockito.Mockito.mock(NewGymPushClaimService.class);
        NotificationsServiceGrpcClient client = org.mockito.Mockito.mock(NotificationsServiceGrpcClient.class);
        when(claim.claim(5L)).thenReturn(null);

        new GymActivationNotifierImpl(client, claim).notifyNewActiveGym(5L, "Sport Life");

        verify(client, never()).notifyNewGym(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void failedSendReleasesTheClaim() {
        NewGymPushClaimService claim = org.mockito.Mockito.mock(NewGymPushClaimService.class);
        NotificationsServiceGrpcClient client = org.mockito.Mockito.mock(NotificationsServiceGrpcClient.class);
        LocalDateTime claimedAt = LocalDateTime.of(2026, 11, 6, 10, 0);
        when(claim.claim(5L)).thenReturn(claimedAt);
        when(client.notifyNewGym(5L, "Sport Life")).thenReturn(false);

        new GymActivationNotifierImpl(client, claim).notifyNewActiveGym(5L, "Sport Life");

        verify(claim, timeout(2000)).release(5L, claimedAt);
    }

    @Test
    void successfulSendKeepsTheClaim() {
        NewGymPushClaimService claim = org.mockito.Mockito.mock(NewGymPushClaimService.class);
        NotificationsServiceGrpcClient client = org.mockito.Mockito.mock(NotificationsServiceGrpcClient.class);
        LocalDateTime claimedAt = LocalDateTime.of(2026, 11, 6, 10, 0);
        when(claim.claim(5L)).thenReturn(claimedAt);
        when(client.notifyNewGym(5L, "Sport Life")).thenReturn(true);

        new GymActivationNotifierImpl(client, claim).notifyNewActiveGym(5L, "Sport Life");

        verify(client, timeout(2000)).notifyNewGym(5L, "Sport Life");
        verify(claim, after(300).never()).release(5L, claimedAt);
    }

    @Test
    void sendWaitsUntilTheActivationTransactionCommits() {
        NewGymPushClaimService claim = org.mockito.Mockito.mock(NewGymPushClaimService.class);
        NotificationsServiceGrpcClient client = org.mockito.Mockito.mock(NotificationsServiceGrpcClient.class);
        LocalDateTime claimedAt = LocalDateTime.of(2026, 11, 6, 10, 0);
        when(claim.claim(5L)).thenReturn(claimedAt);
        when(client.notifyNewGym(5L, "Sport Life")).thenReturn(true);

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            new GymActivationNotifierImpl(client, claim).notifyNewActiveGym(5L, "Sport Life");
            verify(client, never()).notifyNewGym(5L, "Sport Life");
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
            verify(client, timeout(2000)).notifyNewGym(5L, "Sport Life");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }
}

package az.fitnest.catalog.client;

import az.fitnest.notifications.grpc.NotificationsServiceGrpc;
import az.fitnest.notifications.grpc.NotifyNewGymRequest;
import az.fitnest.notifications.grpc.NotifyNewGymResponse;
import az.fitnest.notifications.grpc.SendPushNotificationRequest;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class NotificationsServiceGrpcClient {

    @GrpcClient("notifications-backend")
    private NotificationsServiceGrpc.NotificationsServiceBlockingStub notificationsServiceBlockingStub;

    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "notificationsService")
    public void sendPushNotification(Long userId, String title, String body) {
        try {
            SendPushNotificationRequest request = SendPushNotificationRequest.newBuilder()
                    .setUserId(userId)
                    .setTitle(title)
                    .setBody(body)
                    .putAllData(Collections.emptyMap())
                    .build();
            notificationsServiceBlockingStub
                    .withDeadlineAfter(5, java.util.concurrent.TimeUnit.SECONDS)
                    .sendPushNotification(request);
        } catch (Exception e) {
            // Ignored
        }
    }

    /**
     * Triggers new-gym notification fan-out. Templates live in notifications-backend.
     *
     * @return false when the RPC fails, so the caller can retry on a later activation
     */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "notificationsService")
    public boolean notifyNewGym(Long gymId, String gymName) {
        try {
            NotifyNewGymRequest.Builder builder = NotifyNewGymRequest.newBuilder();
            if (gymId != null) {
                builder.setGymId(gymId);
            }
            if (gymName != null) {
                builder.setGymName(gymName);
            }
            NotifyNewGymResponse response = notificationsServiceBlockingStub
                    .withDeadlineAfter(60, java.util.concurrent.TimeUnit.SECONDS)
                    .notifyNewGym(builder.build());
            return response.getSuccess();
        } catch (Exception e) {
            return false;
        }
    }
}

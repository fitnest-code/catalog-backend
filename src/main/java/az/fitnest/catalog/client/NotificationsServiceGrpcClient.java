package az.fitnest.catalog.client;

import az.fitnest.notifications.grpc.NotificationsServiceGrpc;
import az.fitnest.notifications.grpc.NotifyNewGymRequest;
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

    /** Triggers new-gym notification fan-out; templates/localization live in notifications-backend. */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "notificationsService")
    public void notifyNewGym(Long gymId, String gymName) {
        try {
            NotifyNewGymRequest.Builder builder = NotifyNewGymRequest.newBuilder();
            if (gymId != null) {
                builder.setGymId(gymId);
            }
            if (gymName != null) {
                builder.setGymName(gymName);
            }
            notificationsServiceBlockingStub
                    .withDeadlineAfter(60, java.util.concurrent.TimeUnit.SECONDS)
                    .notifyNewGym(builder.build());
        } catch (Exception e) {
            // Ignored — gym creation must not fail because of notification delivery
        }
    }
}

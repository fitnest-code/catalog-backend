package az.fitnest.catalog.client;

import az.fitnest.notifications.grpc.NotificationsServiceGrpc;
import az.fitnest.notifications.grpc.SendPushNotificationRequest;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class NotificationsServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(NotificationsServiceGrpcClient.class);

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
            logger.error("Failed to send push notification to user {}: {}", userId, e.getMessage(), e);
        }
    }
}

package az.fitnest.catalog.client;

import az.fitnest.notifications.grpc.BroadcastLocalizedPushRequest;
import az.fitnest.notifications.grpc.LocalizedPushContent;
import az.fitnest.notifications.grpc.NotificationsServiceGrpc;
import az.fitnest.notifications.grpc.SendPushNotificationRequest;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

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
     * Broadcasts localized new-gym (or similar) notifications to all ROLE_USER users.
     * Title/body variants are selected per user language on the notifications service.
     */
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(name = "notificationsService")
    public void broadcastLocalizedPushNotification(Map<String, TitleBody> contentsByLanguage,
                                                   Map<String, String> data) {
        try {
            BroadcastLocalizedPushRequest.Builder builder = BroadcastLocalizedPushRequest.newBuilder()
                    .addRoleNames("ROLE_USER");

            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }

            contentsByLanguage.forEach((language, content) ->
                    builder.addContents(LocalizedPushContent.newBuilder()
                            .setLanguage(language)
                            .setTitle(content.title())
                            .setBody(content.body())
                            .build()));

            notificationsServiceBlockingStub
                    .withDeadlineAfter(60, java.util.concurrent.TimeUnit.SECONDS)
                    .broadcastLocalizedPushNotification(builder.build());
        } catch (Exception e) {
            // Ignored — gym creation must not fail because of notification delivery
        }
    }

    public record TitleBody(String title, String body) {
    }
}

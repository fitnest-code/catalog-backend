package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserEventListener {

    private final SavedGymRepository savedGymRepository;
    private final SavedStoreRepository savedStoreRepository;
    private final ReservationRepository reservationRepository;
    private final GymEntranceHistoryRepository gymEntranceHistoryRepository;
    private final ReviewRepository reviewRepository;
    private final GymAdminRepository gymAdminRepository;

    @Transactional
    @KafkaListener(topics = {"user-events"}, groupId = "catalog-backend")
    public void handleUserEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        Object userIdObj = event.get("userId");

        if (userIdObj != null && "USER_HARD_DELETED".equals(eventType)) {
            Long userId = parseUserId(userIdObj);
            if (userId != null) {
                log.warn("Received USER_HARD_DELETED event. Purging all data for userId: {}", userId);

                try {
                    savedGymRepository.deleteByUserId(userId);
                    savedStoreRepository.deleteByUserId(userId);
                    reservationRepository.deleteByUserId(userId);
                    gymEntranceHistoryRepository.deleteByUserId(userId);
                    reviewRepository.deleteByUserId(userId);
                    gymAdminRepository.deleteByUserId(userId);
                    log.info("Successfully purged all catalog data for userId: {}", userId);
                } catch (Exception e) {
                    log.error("Failed to purge catalog data for userId: {}", userId, e);
                }
            }
        }
    }

    private Long parseUserId(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        } else if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}

/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.cache.CacheManager
 *  org.springframework.kafka.annotation.KafkaListener
 *  org.springframework.stereotype.Service
 */
package az.fitnest.catalog.service.impl;

import java.util.Map;

import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class FavoritesEventListener {
    private final CacheManager cacheManager;

    public FavoritesEventListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @KafkaListener(topics = {"favorites-events"}, groupId = "catalog-service")
    public void handleFavoriteEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        Long userId = (Long) event.get("userId");
        String entityType = (String) event.get("entityType");
        if ("FAVORITE_ADDED".equals(eventType) || "FAVORITE_REMOVED".equals(eventType)) {
            this.cacheManager.getCache("favoriteBulkCheck").clear();
        }
    }
}


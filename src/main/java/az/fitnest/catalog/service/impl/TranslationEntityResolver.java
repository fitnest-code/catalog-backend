package az.fitnest.catalog.service.impl;

import org.springframework.stereotype.Component;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TranslationEntityResolver {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TranslationEntityResolver.class);
    private final Map<String, Field> fieldCache = new ConcurrentHashMap<>();

    public Class<?> getEntityClass(String entityType) {
        if (entityType == null) return null;
        switch (entityType.toUpperCase()) {
            case "CATEGORY": return az.fitnest.catalog.model.entity.Category.class;
            case "GYM": return az.fitnest.catalog.model.entity.Gym.class;
            case "STORE": return az.fitnest.catalog.model.entity.Store.class;
            case "TRAINER": return az.fitnest.catalog.model.entity.Trainer.class;
            case "PROFESSION": return az.fitnest.catalog.model.entity.Profession.class;
            case "ROOM": return az.fitnest.catalog.model.entity.Room.class;
            case "GYMADMIN": return az.fitnest.catalog.model.entity.GymAdmin.class;
            case "LESSONTYPE": return az.fitnest.catalog.model.entity.LessonType.class;
            case "GYMLESSONTYPE": return az.fitnest.catalog.model.entity.GymLessonType.class;
            case "GYMSUBSCRIPTION": return az.fitnest.catalog.model.entity.GymSubscription.class;
            case "SUPPORTEDSERVICE":
            case "SUPPORTED_SERVICE":
                return az.fitnest.catalog.model.entity.SupportedService.class;
            default: return null;
        }
    }

    public String extractFieldValue(Object entity, String fieldName) {
        if (entity == null || fieldName == null) return null;
        try {
            Field field = getCachedField(entity.getClass(), fieldName);
            if (field != null) {
                return getFieldValue(entity, field);
            }
            
            for (Field f : entity.getClass().getDeclaredFields()) {
                if (!f.getType().isPrimitive() && !f.getType().getName().startsWith("java.lang") && !f.getType().isEnum()) {
                    f.setAccessible(true);
                    Object child = f.get(entity);
                    if (child != null) {
                        Field childField = getCachedField(child.getClass(), fieldName);
                        if (childField != null) {
                            return getFieldValue(child, childField);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract field value for fieldName: {}", fieldName, e);
        }
        return null;
    }

    private Field getCachedField(Class<?> clazz, String fieldName) {
        String key = clazz.getName() + "#" + fieldName;
        return fieldCache.computeIfAbsent(key, k -> {
            Class<?> current = clazz;
            while (current != null && current != Object.class) {
                try {
                    Field f = current.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException e) {
                    current = current.getSuperclass();
                }
            }
            return null;
        });
    }

    private String getFieldValue(Object target, Field field) throws IllegalAccessException {
        Object val = field.get(target);
        return val != null ? val.toString() : null;
    }
}

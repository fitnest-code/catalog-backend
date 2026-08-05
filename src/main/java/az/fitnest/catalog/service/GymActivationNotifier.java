package az.fitnest.catalog.service;

/**
 * Catalog-side hook after a gym becomes ACTIVE.
 * Does not own notification copy or fan-out — only signals notifications-backend.
 */
public interface GymActivationNotifier {
    void notifyNewActiveGym(Long gymId, String gymName);
}

package az.fitnest.catalog.service;

public interface ReservationAuditService {
    void log(Long reservationId, Long userId, String operation, String oldStatus, String newStatus, String reason);
}

package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.model.entity.ReservationAuditLog;
import az.fitnest.catalog.repository.ReservationAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationAuditService {

    private final ReservationAuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long reservationId, Long userId, String operation, String oldStatus, String newStatus, String reason) {
        ReservationAuditLog log = ReservationAuditLog.builder()
                .reservationId(reservationId)
                .userId(userId)
                .operation(operation)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(reason)
                .build();

        auditLogRepository.save(log);
    }
}

package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.CancelReasonRequest;
import az.fitnest.catalog.dto.CancelReasonResponse;
import az.fitnest.catalog.model.entity.ReservationCancelReason;
import java.util.List;

public interface CancellationReasonService {
    List<CancelReasonResponse> getReasons();
    void createReason(CancelReasonRequest request);
    void updateReason(String code, CancelReasonRequest request);
    void deleteReason(String code);
    ReservationCancelReason getByCode(String code);
}

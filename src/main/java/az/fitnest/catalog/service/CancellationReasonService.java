package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.request.CancelReasonRequest;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.response.CancelReasonResponse;
import az.fitnest.catalog.model.entity.ReservationCancelReason;
import java.util.List;

public interface CancellationReasonService {
    List<CancelReasonResponse> getReasons();
    void createReason(CancelReasonRequest request);
    void updateReason(String code, CancelReasonRequest request);
    void deleteReason(String code);
    ReservationCancelReason getByCode(String code);
}

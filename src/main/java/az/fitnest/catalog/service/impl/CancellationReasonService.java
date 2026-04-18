package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.CancelReasonResponse;
import az.fitnest.catalog.model.entity.ReservationCancelReason;
import az.fitnest.catalog.repository.ReservationCancelReasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CancellationReasonService {

    private final ReservationCancelReasonRepository reasonRepository;

    @Transactional(readOnly = true)
    public List<CancelReasonResponse> getActiveReasons() {
        return reasonRepository.findByStatusOrderByCreatedDateAsc("ACTIVE")
                .stream()
                .map(reason -> CancelReasonResponse.builder()
                        .code(reason.getCode())
                        .label(reason.getLabel())
                        .requiresComment(reason.getRequiresComment())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReservationCancelReason getByCode(String code) {
        return reasonRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("INVALID_CANCEL_REASON"));
    }
}

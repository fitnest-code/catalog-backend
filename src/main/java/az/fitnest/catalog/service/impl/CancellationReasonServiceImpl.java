package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.request.CancelReasonRequest;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.response.CancelReasonResponse;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.ReservationCancelReason;
import az.fitnest.catalog.repository.ReservationCancelReasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CancellationReasonServiceImpl implements az.fitnest.catalog.service.CancellationReasonService {

    private final ReservationCancelReasonRepository reasonRepository;

    @Transactional(readOnly = true)
    public List<CancelReasonResponse> getReasons() {
        return reasonRepository.findAll()
                .stream()
                .map(reason -> CancelReasonResponse.builder()
                        .code(reason.getCode())
                        .label(reason.getLabel())
                        .requiresComment(reason.getRequiresComment())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void createReason(CancelReasonRequest request) {
        if (reasonRepository.findByCode(request.code()).isPresent()) {
            throw new IllegalArgumentException("REASON_CODE_EXISTS");
        }
        ReservationCancelReason reason = ReservationCancelReason.builder()
                .code(request.code())
                .label(request.label())
                .requiresComment(request.requiresComment())
                .build();
        reasonRepository.save(reason);
    }

    @Transactional
    public void updateReason(String code, CancelReasonRequest request) {
        ReservationCancelReason reason = reasonRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("REASON_NOT_FOUND", "error.reason_not_found"));
        reason.setLabel(request.label());
        reason.setRequiresComment(request.requiresComment());
        reasonRepository.save(reason);
    }

    @Transactional
    public void deleteReason(String code) {
        ReservationCancelReason reason = reasonRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("REASON_NOT_FOUND", "error.reason_not_found"));
        reasonRepository.delete(reason);
    }

    @Transactional(readOnly = true)
    public ReservationCancelReason getByCode(String code) {
        return reasonRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("INVALID_CANCEL_REASON"));
    }
}

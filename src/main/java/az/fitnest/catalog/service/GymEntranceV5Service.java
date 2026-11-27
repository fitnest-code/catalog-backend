package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.response.GymEntranceEligibilityResponse;
import az.fitnest.catalog.dto.v5.GymEntranceEligibilityRequestV5;
import az.fitnest.catalog.dto.v5.GymEntranceEligibilityResponseV5;
import az.fitnest.catalog.dto.v5.GymEntranceScanRequestV5;
import az.fitnest.catalog.dto.v5.GymEntranceScanResponseV5;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.service.category.CategoryEntryPriceResolver;
import az.fitnest.catalog.service.category.GymCategoryOfferingsService;
import az.fitnest.catalog.service.impl.GymEntranceServiceImpl;
import az.fitnest.catalog.util.UserContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * v5 QR entrance. Delegates all rule/limit/check-in logic to the classic {@link GymEntranceService}; this
 * layer only adds category validation (picker required when the gym has subs / several categories) and the
 * category-aware response envelope. Old v1/v4 entrance contracts stay untouched.
 */
@Service
@RequiredArgsConstructor
public class GymEntranceV5Service {

    private final GymEntranceService gymEntranceService;
    private final GymRepository gymRepository;
    private final GymCategoryOfferingsService offerings;
    private final CategoryEntryPriceResolver priceResolver;

    @Transactional(readOnly = true)
    public GymEntranceEligibilityResponseV5 checkEligibility(Object principal, GymEntranceEligibilityRequestV5 request) {
        GymEntranceEligibilityResponse base = gymEntranceService.checkGymEntranceEligibility(principal);

        Long gymId = null;
        if (request != null) {
            gymId = request.gymId() != null
                    ? request.gymId()
                    : GymEntranceServiceImpl.extractGymIdFromQr(request.qrCodeValue());
        }
        Gym gym = gymId == null ? null : gymRepository.findWithDetailsById(gymId).orElse(null);
        if (gym == null) {
            // Gym-agnostic call (same as v1): nothing to pick from yet.
            return new GymEntranceEligibilityResponseV5(base.allowed(), base.reason(), false, List.of());
        }
        String lang = UserContext.getUserLanguage();
        List<GymEntranceEligibilityResponseV5.CategoryChip> chips = GymCategoryOfferingsService.offeredCategories(gym)
                .stream()
                .map(e -> chip(e.getKey(), e.getValue(), lang))
                .toList();
        return new GymEntranceEligibilityResponseV5(
                base.allowed(), base.reason(), GymCategoryOfferingsService.categorySelectionRequired(gym), chips);
    }

    // Not @Transactional: the delegate owns the scan transaction; validation reads use their own short reads.
    public GymEntranceScanResponseV5 scan(Object principal, GymEntranceScanRequestV5 request, String userAgent) {
        Long gymId = GymEntranceServiceImpl.extractGymIdFromQr(request.qrCodeValue());
        Gym gym = gymId == null ? null : gymRepository.findWithDetailsById(gymId).orElse(null);
        // Unknown gym / bad QR: let the classic flow produce its own error (invalid QR / GYM_NOT_FOUND).
        if (gym != null) {
            validateCategory(gym, request.categoryId());
        }

        var outcome = gymEntranceService.scanGymQrEntrance(principal, request.qrCodeValue(), request.lat(),
                request.lng(), userAgent, request.confirmation(), request.confirmationToken(),
                request.qrScanId(), request.categoryId());

        String categoryName = null;
        if (gym != null && request.categoryId() != null) {
            String lang = UserContext.getUserLanguage();
            categoryName = GymCategoryOfferingsService.offeredCategories(gym).stream()
                    .filter(e -> request.categoryId().equals(e.getKey().getId()))
                    .map(e -> offerings.localizedCategoryName(e.getKey(), lang))
                    .findFirst()
                    .orElse(null);
        }
        boolean entered = outcome.response() != null && outcome.response().isAllowed();
        return new GymEntranceScanResponseV5(
                outcome.response(), request.categoryId(), categoryName, entered ? outcome.amount() : null);
    }

    /** Shared with the Primary Gym v4-style path: category mandatory only when the gym needs a selection. */
    public void validateCategory(Gym gym, Long categoryId) {
        if (categoryId == null) {
            if (GymCategoryOfferingsService.categorySelectionRequired(gym)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CATEGORY_ID_REQUIRED");
            }
            return;
        }
        priceResolver.requireOwnedCategory(gym, categoryId);
    }

    private GymEntranceEligibilityResponseV5.CategoryChip chip(Category c, String role, String lang) {
        return new GymEntranceEligibilityResponseV5.CategoryChip(
                c.getId(), offerings.localizedCategoryName(c, lang), role, c.getPhotoUrl());
    }
}

package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.v5.GymDetailResponseV5;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.service.category.GymCategoryOfferingsService;
import az.fitnest.catalog.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gym detail v5 = untouched v4 payload + category offerings. v4 contract is not modified. */
@Service
@RequiredArgsConstructor
public class GymDetailV5Service {

    private final GymReadService gymReadService;
    private final GymRepository gymRepository;
    private final GymCategoryOfferingsService offerings;

    @Transactional(readOnly = true)
    public GymDetailResponseV5 getGymDetail(Long userId, Long gymId) {
        var gym = gymRepository.findWithDetailsById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        var detail = gymReadService.getGymDetailV4(userId, gymId);
        return new GymDetailResponseV5(
                detail,
                GymCategoryOfferingsService.hasSubcategories(gym),
                GymCategoryOfferingsService.categorySelectionRequired(gym),
                offerings.buildOfferings(gym, UserContext.getUserLanguage()));
    }
}

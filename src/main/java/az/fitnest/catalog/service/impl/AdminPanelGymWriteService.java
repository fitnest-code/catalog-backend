package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.admin.AdminPanelCreateGymRequest;
import az.fitnest.catalog.dto.admin.AdminPanelGeocodingResponse;
import az.fitnest.catalog.dto.admin.AdminPanelGymResponse;
import az.fitnest.catalog.dto.admin.GeneralInfoRequest;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ConflictException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.mapper.AdminPanelGymMapper;
import az.fitnest.catalog.model.entity.GymAdminPanel;
import az.fitnest.catalog.model.enums.AdminPanelGymStatus;
import az.fitnest.catalog.repository.GymAdminPanelRepository;
import az.fitnest.catalog.service.AdminPanelReverseGeocodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPanelGymWriteService {

    private final GymAdminPanelRepository gymAdminPanelRepository;
    private final LocationService locationService;
    private final AdminPanelReverseGeocodingService reverseGeocodingService;
    private final AdminPanelGymMapper gymAdminMapper;

    @Transactional
    public AdminPanelGymResponse createGymForAdmin(AdminPanelCreateGymRequest request) {
        GymAdminPanel saved = gymAdminPanelRepository.save(gymAdminMapper.toEntity(request));
        return gymAdminMapper.toCreateResponse(saved);
    }

    @Transactional
    public void deleteGym(Long gymId) {
        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        if (gym.getSubscriptions() != null && !gym.getSubscriptions().isEmpty()) {
            throw new ConflictException("GYM_HAS_ACTIVE_SUBSCRIPTIONS", "error.gym_has_active_subscriptions");
        }

        gym.setStatus(AdminPanelGymStatus.DELETED);
        gymAdminPanelRepository.save(gym);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = {"gym-detail", "main-page-gyms"}, allEntries = true)
    })
    public void updateGeneralInfo(Long gymId, GeneralInfoRequest request) {
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new BadRequestException("INVALID_LOCATION", "error.lat_lng_must_be_together");
        }

        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        AdminPanelGeocodingResponse geocoding = reverseGeocodingService
                .reverseGeocode(request.latitude(), request.longitude());

        gymAdminMapper.updateGeneralInfo(gym, request, geocoding);

        if (request.latitude() != null) {
            locationService.resolveAndSetLocation(gym.getAddress(), geocoding);
        }

        gymAdminPanelRepository.save(gym);
    }

}

package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.SortDirection;
import az.fitnest.catalog.dto.admin.*;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.mapper.AdminPanelGymMapper;
import az.fitnest.catalog.model.entity.GymAdminPanel;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.repository.AdminPanelWorkingHourRepository;
import az.fitnest.catalog.repository.GymAdminPanelRepository;
import az.fitnest.catalog.repository.TrainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminPanelGymReadService {

    private final AdminPanelWorkingHourRepository workingHourRepository;
    private final GymAdminPanelRepository gymAdminPanelRepository;
    private final AdminPanelGymMapper adminPanelGymMapper;
    private final TrainerRepository trainerRepository;
    private final LocationService locationService;

    @Transactional(readOnly = true)
    public PaginatedResponse<AdminGymListDto> getGymsForAdmin(
            String search, GymStatus status,
            Long cityId, Long districtId,
            String sortBy, SortDirection sortOrder,
            int page, int size) {


        Set<String> allowed = Set.of("name", "createdAt", "status");
        if (!allowed.contains(sortBy)) sortBy = "createdAt";

        String cityName = locationService.resolveCityName(cityId);
        String districtName = locationService.resolveDistrictName(cityId, districtId);

        Pageable pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(Sort.Direction.fromString(sortOrder.name()), sortBy)
        );

        Page<AdminGymListDto> result = gymAdminPanelRepository
                .findAllForAdmin(search, status, cityName, districtName, pageable);

        return PaginatedResponse.of(result);
    }

    @Transactional(readOnly = true)
    public AdminPanelGymDetailDto getGymForAdmin(Long gymId) {
        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        return adminPanelGymMapper.toDetailDto(gym);
    }

    @Transactional(readOnly = true)
    public List<AdminPanelGymImageDto> getGalleryImages(Long gymId) {
        GymAdminPanel gym = gymAdminPanelRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));

        return gym.getImages().stream()
                .sorted(Comparator.comparingInt(i -> i.getSortOrder() != null ? i.getSortOrder() : 0))
                .map(i -> new AdminPanelGymImageDto(i.getId(), i.getUrl(), i.getSortOrder()))
                .toList();
    }

    public List<WorkingHourDto> getWorkingHours(Long gymId) {
        return workingHourRepository.findAllByGymIdOrderByDayOfWeekAsc(gymId)
                .stream()
                .map(adminPanelGymMapper::toWorkingHourDto)
                .toList();
    }

    public PaginatedResponse<TrainerListDto> getTrainers(Long gymId, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("firstName").ascending());
        Page<Trainer> result = trainerRepository.findAllByGymId(gymId, search, pageable);

        Page<TrainerListDto> mapped = result.map(adminPanelGymMapper::toTrainerListDto);

        return PaginatedResponse.of(mapped);
    }

}

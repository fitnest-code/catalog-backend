package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.SortDirection;
import az.fitnest.catalog.dto.admin.AdminGymListDto;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.repository.GymAdminPanelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminPanelGymReadService {

    private final GymAdminPanelRepository gymAdminPanelRepository;
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
}

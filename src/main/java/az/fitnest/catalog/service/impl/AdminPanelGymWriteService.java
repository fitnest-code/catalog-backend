package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.admin.AdminPanelCreateGymRequest;
import az.fitnest.catalog.dto.admin.AdminPanelGymResponse;
import az.fitnest.catalog.model.entity.GymAdminPanel;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.repository.GymAdminPanelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPanelGymWriteService {

    private final GymAdminPanelRepository gymAdminPanelRepository;

    @Transactional
    public AdminPanelGymResponse createGymForAdmin(AdminPanelCreateGymRequest request) {
        GymAdminPanel gym = GymAdminPanel.builder()
                .name(request.name())
                .status(GymStatus.INACTIVE)
                .build();

        GymAdminPanel saved = gymAdminPanelRepository.save(gym);

        return new AdminPanelGymResponse(
                saved.getId(),
                saved.getName(),
                saved.getStatus()
        );
    }
}

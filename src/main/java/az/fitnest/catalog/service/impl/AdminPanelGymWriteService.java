package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.admin.AdminPanelCreateGymRequest;
import az.fitnest.catalog.dto.admin.AdminPanelGymResponse;
import az.fitnest.catalog.exception.ConflictException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.GymAdminPanel;
import az.fitnest.catalog.model.enums.AdminPanelGymStatus;
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
                .status(AdminPanelGymStatus.INACTIVE)
                .build();

        GymAdminPanel saved = gymAdminPanelRepository.save(gym);

        return new AdminPanelGymResponse(
                saved.getId(),
                saved.getName(),
                saved.getStatus()
        );
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


}

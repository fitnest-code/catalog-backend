package az.fitnest.catalog.mapper;

import az.fitnest.catalog.dto.admin.*;
import az.fitnest.catalog.model.entity.AddressAdminPanel;
import az.fitnest.catalog.model.entity.GymAdminPanel;
import az.fitnest.catalog.model.enums.AdminPanelGymStatus;
import org.springframework.stereotype.Component;

@Component
public final class AdminPanelGymMapper {

    public GymAdminPanel toEntity(AdminPanelCreateGymRequest request) {
        return GymAdminPanel.builder()
                .name(request.name())
                .status(AdminPanelGymStatus.INACTIVE)
                .build();
    }

    public AdminPanelGymResponse toCreateResponse(GymAdminPanel gym) {
        return new AdminPanelGymResponse(
                gym.getId(),
                gym.getName(),
                gym.getStatus()
        );
    }

    public AdminPanelGymDetailDto toDetailDto(GymAdminPanel gym) {
        return new AdminPanelGymDetailDto(
                gym.getId(),
                gym.getName(),
                gym.getDescription(),
                gym.getStatus(),
                gym.getPhone(),
                gym.getEmail(),
                gym.getAddress() != null ? gym.getAddress().getAddressText() : null,
                gym.getAddress() != null ? gym.getAddress().getLatitude() : null,
                gym.getAddress() != null ? gym.getAddress().getLongitude() : null,
                gym.getCoverImageUrl()
        );
    }

    public void updateGeneralInfo(GymAdminPanel gym, GeneralInfoRequest request,
                                  AdminPanelGeocodingResponse geocoding) {
        gym.setName(request.name());
        gym.setDescription(request.description());
        gym.setPhone(request.phoneNumber());
        gym.setEmail(request.email());

        if (request.latitude() != null) {
            AddressAdminPanel address = gym.getAddress() != null ? gym.getAddress() : new AddressAdminPanel();
            address.setLatitude(request.latitude());
            address.setLongitude(request.longitude());
            address.setAddressText(geocoding != null ? geocoding.addressText() : request.address());
            gym.setAddress(address);
        }
    }
}

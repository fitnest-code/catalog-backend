package az.fitnest.catalog.mapper;

import az.fitnest.catalog.dto.admin.*;
import az.fitnest.catalog.model.entity.*;
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

    public WorkingHourDto toWorkingHourDto(AdminPanelWorkingHour wh) {
        return new WorkingHourDto(
                wh.getId(),
                wh.getDayOfWeek(),
                resolveDayLabel(wh.getDayOfWeek()),
                wh.getOpenTime() != null ? wh.getOpenTime().toString() : null,
                wh.getCloseTime() != null ? wh.getCloseTime().toString() : null,
                wh.getIsClosed()
        );
    }

    public void updateWorkingHour(AdminPanelWorkingHour wh, WorkingHourRequest request) {
        wh.setDayOfWeek(request.dayOfWeek());
        wh.setOpenTime(parseTime(request.openTime()));
        wh.setCloseTime(parseTime(request.closeTime()));
        wh.setIsClosed(request.isClosed());
    }

    public TrainerListDto toTrainerListDto(Trainer t) {
        return new TrainerListDto(
                t.getId(),
                t.getFirstName(),
                t.getLastName(),
                t.getSpecialization(),
                t.getPhone(),
                t.getEmail(),
                t.getProfileImageUrl()
        );
    }

    public TrainerDetailDto toDetailDto(Trainer t) {
        return new TrainerDetailDto(
                t.getId(),
                t.getFirstName(),
                t.getLastName(),
                t.getSpecialization(),
                t.getExperienceYears(),
                t.getPhone(),
                t.getEmail(),
                t.getProfileImageUrl(),
                t.getRating()
        );
    }

    public void updateTrainer(Trainer trainer, AdminPanelTrainerRequest request) {
        trainer.setFirstName(request.firstName());
        trainer.setLastName(request.lastName());
        trainer.setSpecialization(request.specialization());
        trainer.setPhone(request.phoneNumber());
        trainer.setEmail(request.email());
    }

    public GymAdminListDto adminListDto(AdminPanelGymAdmin admin) {
        return new GymAdminListDto(
                admin.getId(),
                admin.getFirstName(),
                admin.getLastName(),
                admin.getPhoneNumber(),
                admin.getEmail(),
                admin.getRole(),
                admin.getStatus()
        );
    }

    private java.time.LocalTime parseTime(String time) {
        return time != null ? java.time.LocalTime.parse(time) : null;
    }

    private String resolveDayLabel(Integer dayOfWeek) {
        if (dayOfWeek == null) {
            return null;
        }

        return switch (dayOfWeek) {
            case 1 -> "Bazar ertəsi";
            case 2 -> "Çərşənbə axşamı";
            case 3 -> "Çərşənbə";
            case 4 -> "Cümə axşamı";
            case 5 -> "Cümə";
            case 6 -> "Şənbə";
            case 7 -> "Bazar";
            default -> null;
        };
    }

}

package az.fitnest.catalog.mapper;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.model.entity.*;
import java.util.List;
import java.util.stream.Collectors;

public final class GymMapper {

    private GymMapper() {}

    public static GymTrainerDto toTrainerDto(Trainer t) {
        if (t == null) return null;
        ProfessionDto professionDto = null;
        if (t.getProfession() != null) {
            professionDto = ProfessionDto.builder()
                    .id(t.getProfession().getId())
                    .name(t.getProfession().getName())
                    .build();
        }
        
        return GymTrainerDto.builder()
                .trainer_id(t.getId() != null ? t.getId().toString() : null)
                .name(t.getName())
                .surname(t.getSurname())
                .profession(professionDto)
                .picture(t.getPicture())
                .phone(t.getPhone())
                .email(t.getEmail())
                .build();
    }

    public static GymReviewDto toReviewDto(Review r) {
        if (r == null) return null;
        return GymReviewDto.builder()
                .review_id(r.getId() != null ? r.getId().toString() : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .created_at(r.getCreatedDate())
                .author(GymReviewAuthorDto.builder()
                        .user_id(r.getUserId() != null ? r.getUserId().toString() : null)
                        .full_name("User " + r.getUserId())
                        .build())
                .build();
    }

    public static GymImageDto toImageDto(GymImage img) {
        if (img == null) return null;
        return GymImageDto.builder()
                .id(img.getId())
                .gymId(img.getGym() != null ? img.getGym().getId() : null)
                .name(img.getImageName())
                .url(img.getUrl())
                .build();
    }

    public static GymImageItemDto toImageItemDto(GymImage img) {
        if (img == null) return null;
        return GymImageItemDto.builder()
                .image_id(img.getId() != null ? img.getId().toString() : "img_" + System.identityHashCode(img))
                .url(img.getUrl())
                .type(img.getType() != null ? img.getType() : "other")
                .title(img.getTitle())
                .build();
    }

    public static GymWorkHourDto toWorkHourDto(GymWorkHour wh) {
        if (wh == null) return null;
        return GymWorkHourDto.builder()
                .day(wh.getDay())
                .from(wh.getFromTime())
                .to(wh.getToTime())
                .build();
    }
}

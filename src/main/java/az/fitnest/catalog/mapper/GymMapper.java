package az.fitnest.catalog.mapper;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.model.entity.*;

import java.util.List;
import java.util.stream.Collectors;

public final class GymMapper {

    private GymMapper() {
    }

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
                .created_at(r.getCreatedDate() != null ? r.getCreatedDate().toLocalDate() : null)
                .author(GymReviewAuthorDto.builder()
                        .user_id(r.getUserId() != null ? r.getUserId().toString() : null)
                        .full_name("User " + r.getUserId())
                        .build())
                .build();
    }

    public static GymReviewDto toReviewDto(Review r, String fullName, String profileImageUrl) {
        if (r == null) return null;
        return GymReviewDto.builder()
                .review_id(r.getId() != null ? r.getId().toString() : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .created_at(r.getCreatedDate() != null ? r.getCreatedDate().toLocalDate() : null)
                .author(GymReviewAuthorDto.builder()
                        .user_id(r.getUserId() != null ? r.getUserId().toString() : null)
                        .full_name(fullName)
                        .avatar_url(profileImageUrl)
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

    private static final java.util.Map<az.fitnest.catalog.model.enums.GymWorkHourPeriod, String> AZ_DAYS = java.util.Map.of(
            az.fitnest.catalog.model.enums.GymWorkHourPeriod.MONDAY, "Bazar ertəsi",
            az.fitnest.catalog.model.enums.GymWorkHourPeriod.TUESDAY, "Çərşənbə axşamı",
            az.fitnest.catalog.model.enums.GymWorkHourPeriod.WEDNESDAY, "Çərşənbə",
            az.fitnest.catalog.model.enums.GymWorkHourPeriod.THURSDAY, "Cümə axşamı",
            az.fitnest.catalog.model.enums.GymWorkHourPeriod.FRIDAY, "Cümə",
            az.fitnest.catalog.model.enums.GymWorkHourPeriod.SATURDAY, "Şənbə",
            az.fitnest.catalog.model.enums.GymWorkHourPeriod.SUNDAY, "Bazar"
    );

    public static List<GymWorkHourDto> toGroupedWorkHourDtos(java.util.Collection<GymWorkHour> workHours) {
        if (workHours == null || workHours.isEmpty()) return java.util.Collections.emptyList();

        List<GymWorkHour> sorted = workHours.stream()
                .sorted(java.util.Comparator.comparingInt(wh -> wh.getPeriod().ordinal()))
                .toList();

        List<GymWorkHourDto> grouped = new java.util.ArrayList<>();
        int i = 0;
        while (i < sorted.size()) {
            GymWorkHour start = sorted.get(i);
            int j = i + 1;
            while (j < sorted.size() &&
                    sorted.get(j).getPeriod().ordinal() == sorted.get(j - 1).getPeriod().ordinal() + 1 &&
                    java.util.Objects.equals(sorted.get(j).getFromTime(), start.getFromTime()) &&
                    java.util.Objects.equals(sorted.get(j).getToTime(), start.getToTime())) {
                j++;
            }

            String periodStr;
            if (j - i > 1) {
                periodStr = AZ_DAYS.get(start.getPeriod()) + " – " + AZ_DAYS.get(sorted.get(j - 1).getPeriod());
            } else {
                periodStr = AZ_DAYS.get(start.getPeriod());
            }

            grouped.add(GymWorkHourDto.builder()
                    .period(periodStr)
                    .from(start.getFromTime())
                    .to(start.getToTime())
                    .build());
            i = j;
        }

        return grouped;
    }

    public static GymWorkHourDto toWorkHourDto(GymWorkHour wh) {
        if (wh == null) return null;
        return GymWorkHourDto.builder()
                .period(wh.getPeriod() != null ? AZ_DAYS.getOrDefault(wh.getPeriod(), wh.getPeriod().name()) : null)
                .from(wh.getFromTime())
                .to(wh.getToTime())
                .build();
    }

    public static CategoryDto toCategoryDto(Category category) {
        if (category == null) return null;
        return CategoryDto.builder()
                .id(category.getCategoryId())
                .name(category.getName())
                .photoUrl(category.getPhotoUrl())
                .build();
    }
}

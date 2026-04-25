package az.fitnest.catalog.dto.admin;

public record TrainerDetailDto(
        Long id,
        String firstName,
        String lastName,
        String specialization,
        Integer experienceYears,
        String phoneNumber,
        String email,
        String imageUrl,
        Double rating
) {
}

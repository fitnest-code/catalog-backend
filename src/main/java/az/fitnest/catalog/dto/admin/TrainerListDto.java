package az.fitnest.catalog.dto.admin;

public record TrainerListDto(
        Long id,
        String firstName,
        String lastName,
        String specialization,
        String phoneNumber,
        String email,
        String imageUrl
) {
}

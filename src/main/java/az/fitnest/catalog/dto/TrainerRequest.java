package az.fitnest.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update a trainer")
public class TrainerRequest {

    @NotBlank(message = "Məşqçinin adı tələb olunur")
    @Schema(description = "Trainer's first name", example = "John")
    private String name;

    @NotBlank(message = "Məşqçinin soyadı tələb olunur")
    @Schema(description = "Trainer's last name", example = "Doe")
    private String surname;

    @NotNull(message = "Peşə ID-si tələb olunur")
    @Schema(description = "ID of the assigned Profession entity", example = "1")
    private Long professionId;

    @Schema(description = "Profile image URL")
    private String picture;

    @Schema(description = "Contact phone number", example = "+1234567890")
    private String phone;

    @Schema(description = "Contact email address", example = "john.doe@example.com")
    private String email;
}


package az.fitnest.catalog.dto.request;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.*;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
@Schema(description = "Request to create or update a trainer")
public record TrainerRequest(
    @NotBlank(message = "Məşqçinin adı tələb olunur")
    @Schema(description = "Trainer's first name", example = "John")
    String name,

    @NotBlank(message = "Məşqçinin soyadı tələb olunur")
    @Schema(description = "Trainer's last name", example = "Doe")
    String surname,

    @NotNull(message = "Peşə ID-si tələb olunur")
    @Schema(description = "ID of the assigned Profession entity", example = "1")
    Long professionId,

    @Schema(description = "Contact phone number", example = "+1234567890")
    String phone,

    @Schema(description = "Contact email address", example = "john.doe@example.com")
    String email
) {}

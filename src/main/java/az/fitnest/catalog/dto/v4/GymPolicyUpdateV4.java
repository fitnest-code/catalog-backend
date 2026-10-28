package az.fitnest.catalog.dto.v4;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;

public record GymPolicyUpdateV4(
    @NotNull @Valid GymMembershipPolicyV4 policy,
    boolean notifySubscribers,
    Map<String, @NotNull @Valid Content> notification) {
  public record Content(
      @NotBlank @Size(max = 65) String title, @NotBlank @Size(max = 240) String body) {}
}

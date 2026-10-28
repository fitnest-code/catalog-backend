package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.v4.GymMembershipPolicyV4;
import az.fitnest.catalog.dto.v4.GymPolicyUpdateV4;
import az.fitnest.catalog.service.GymMembershipPolicyV4Service;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GymMembershipPolicyV4Controller {
  private final GymMembershipPolicyV4Service service;

  @GetMapping("/api/v4/admin/gyms/{gymId}/membership-policy")
  @PreAuthorize("hasRole('ADMIN')")
  public GymMembershipPolicyV4 get(@PathVariable Long gymId) {
    return service.get(gymId);
  }

  @PutMapping("/api/v4/admin/gyms/{gymId}/membership-policy")
  @PreAuthorize("hasRole('ADMIN')")
  public GymMembershipPolicyV4 save(
      @PathVariable Long gymId, @Valid @RequestBody GymPolicyUpdateV4 input) {
    return service.save(gymId, input);
  }

  @GetMapping("/api/v4/gyms/{gymId}/membership-quote")
  public GymMembershipPolicyV4.Quote quote(@PathVariable Long gymId, @RequestParam Long packageId) {
    return service.quote(gymId, packageId, LocalDateTime.now(GymMembershipPolicyV4Service.ZONE));
  }
}

package az.fitnest.catalog.controller;

import az.fitnest.catalog.service.GymEntranceV6Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v6 gym entrance. Category visit gates (SUB ≤12 at primary, MAIN ≤30) are enforced in order-backend
 * v6; this controller only validates QR/location and forwards priced check-ins.
 */
@RestController
@RequestMapping("/api/v6/gyms")
@RequiredArgsConstructor
@Tag(name = "Gyms V6", description = "Primary check-in with MAIN/SUB visit limits")
@SecurityRequirement(name = "bearerAuth")
public class GymV6Controller {

  private final GymEntranceV6Service entranceService;

  @PostMapping("/entrance/primary-scan")
  @Operation(
      summary = "Primary gym check-in v6",
      description =
          "Requires categoryId when the gym has subcategory offerings. SUB visits share the first "
              + "12 primary slots; slots 13–30 are MAIN only. Subcategory price is single-entry.")
  public Object primaryScan(@Valid @RequestBody GymEntranceV6Service.Scan input) {
    return entranceService.primaryScan(input);
  }
}

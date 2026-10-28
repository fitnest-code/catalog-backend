package az.fitnest.catalog.controller;

import az.fitnest.catalog.service.GymEntranceV4Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GymEntranceV4Controller {
  private final GymEntranceV4Service service;

  @PostMapping("/api/v4/gyms/entrance/scan")
  public Object scan(@Valid @RequestBody GymEntranceV4Service.Scan input) {
    return service.scan(input);
  }
}

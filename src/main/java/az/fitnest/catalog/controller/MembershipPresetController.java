package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.MembershipPresetDto;
import az.fitnest.catalog.service.MembershipPresetsProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Membership Presets", description = "Endpoints for retrieving predefined membership plans")
public class MembershipPresetController {

    @GetMapping("/membership-presets")
    @Operation(summary = "Get membership presets", description = "Returns the list of predefined membership plan templates.")
    public ResponseEntity<List<MembershipPresetDto>> getMembershipPresets() {
        return ResponseEntity.ok(MembershipPresetsProvider.getPresets());
    }
}

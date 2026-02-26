package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.model.entity.Store;
import az.fitnest.catalog.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
@Tag(name = "Stores", description = "Endpoints for viewing catalog stores and user interactions with stores.")
public class StoreController {

    private final StoreService storeService;

    @Operation(summary = "Get catalog stores", description = "Consolidated endpoint for all store listings (All, Closest, New, Discounted, Saved). Use 'type' parameter to switch views.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stores retrieved successfully", content = {@Content(schema = @Schema(implementation = PaginatedResponse.class))}),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping
    public ResponseEntity<PaginatedResponse<StoreMainPageDto>> getStores(
            @Parameter(description = "Search query (matches name or address)") @RequestParam(value = "q", required = false) String q,
            @Parameter(description = "Filter type (ALL, NEW, DISCOUNTED, CLOSEST, SAVED)") @RequestParam(value = "type", defaultValue = "ALL") String type,
            @Parameter(description = "User latitude (required for CLOSEST type)") @RequestParam(value = "lat", required = false) Double lat,
            @Parameter(description = "User longitude (required for CLOSEST type)") @RequestParam(value = "lng", required = false) Double lng,
            @Parameter(description = "Page index (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Items per page") @RequestParam(defaultValue = "10") int page_size) {
        Long userId = this.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getStores(userId, q, type, lat, lng, page, page_size));
    }

    @Operation(summary = "Get store details", description = "Retrieves full details of a specific store.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Store details retrieved successfully", content = {@Content(schema = @Schema(implementation = StoreDetailResponseDto.class))}),
            @ApiResponse(responseCode = "404", description = "Store not found")
    })
    @GetMapping("/{storeId:\\d+}")
    public ResponseEntity<StoreDetailResponseDto> getStoreDetail(@Parameter(description = "ID of the store") @PathVariable Long storeId) {
        Long userId = this.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getStoreDetail(userId, storeId));
    }

    @Operation(summary = "Get store location", description = "Returns the coordinates and text of a specific store.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Store location retrieved", content = {@Content(schema = @Schema(implementation = LocationDto.class))}),
            @ApiResponse(responseCode = "404", description = "Store not found")
    })
    @GetMapping("/{storeId:\\d+}/location")
    public ResponseEntity<LocationDto> getStoreLocation(@Parameter(description = "ID of the store") @PathVariable Long storeId) {
        return ResponseEntity.ok(this.storeService.getStoreLocation(storeId));
    }

    @Operation(summary = "Save/Unsave store", description = "Toggles the 'saved' status of a store.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Toggle successful"),
            @ApiResponse(responseCode = "404", description = "Store not found")
    })
    @PostMapping("/{storeId}/save")
    public ResponseEntity<Map<String, Boolean>> saveStore(@PathVariable Long storeId) {
        Long userId = this.getCurrentUserId();
        boolean isSaved = this.storeService.toggleSave(userId, storeId);
        return ResponseEntity.ok(Map.of("is_saved", isSaved));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        return null;
    }
}

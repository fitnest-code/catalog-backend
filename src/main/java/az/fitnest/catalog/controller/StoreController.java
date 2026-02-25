/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.Operation
 *  io.swagger.v3.oas.annotations.Parameter
 *  io.swagger.v3.oas.annotations.media.Content
 *  io.swagger.v3.oas.annotations.media.ExampleObject
 *  io.swagger.v3.oas.annotations.media.Schema
 *  io.swagger.v3.oas.annotations.responses.ApiResponse
 *  io.swagger.v3.oas.annotations.responses.ApiResponses
 *  io.swagger.v3.oas.annotations.security.SecurityRequirement
 *  io.swagger.v3.oas.annotations.tags.Tag
 *  org.springframework.http.ResponseEntity
 *  org.springframework.security.access.prepost.PreAuthorize
 *  org.springframework.security.core.Authentication
 *  org.springframework.security.core.context.SecurityContextHolder
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.StoreDetailResponseDto;
import az.fitnest.catalog.dto.StoreMainPageDto;
import az.fitnest.catalog.dto.LocationDto;
import az.fitnest.catalog.dto.StoreRequest;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
@Tag(name = "Stores", description = "Endpoints for viewing catalog stores and user interactions with stores.")
public class StoreController {

    private final StoreService storeService;

    @Operation(summary = "Get catalog stores", description = "Returns a paginated list of stores. Search (q) matches only store name and address.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Stores retrieved successfully", content = {@Content(schema = @Schema(implementation = PaginatedResponse.class))}), @ApiResponse(responseCode = "401", description = "Authentication required")})
    @GetMapping
    public ResponseEntity<PaginatedResponse<StoreMainPageDto>> getMarketStores(@Parameter(description = "Search query (matches name or address)") @RequestParam(value = "q", required = false) String q, @Parameter(description = "Page index (1-based)") @RequestParam(defaultValue = "1") int page, @Parameter(description = "Items per page") @RequestParam(defaultValue = "10") int page_size) {
        Long userId = this.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getStoreMainPage(userId, q, page, page_size));
    }

    @Operation(summary = "Get closest stores", description = "Returns paginated list of stores closest to provided coordinates.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Closest stores retrieved", content = {@Content(schema = @Schema(implementation = PaginatedResponse.class))}), @ApiResponse(responseCode = "401", description = "Authentication required")})
    @GetMapping("/closest")
    public ResponseEntity<PaginatedResponse<StoreMainPageDto>> getClosestStores(@Parameter(description = "Search query") @RequestParam(value = "q", required = false) String q, @Parameter(description = "Page index (1-based)") @RequestParam(defaultValue = "1") int page, @Parameter(description = "Items per page") @RequestParam(defaultValue = "10") int page_size, @Parameter(description = "User latitude") @RequestParam(value = "lat", required = false) Double lat, @Parameter(description = "User longitude") @RequestParam(value = "lng", required = false) Double lng) {
        Long userId = this.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getClosestStores(userId, q, page, page_size, lat, lng));
    }

    @Operation(summary = "Get store details", description = "Retrieves full details of a specific store, including products.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Store details retrieved successfully", content = {@Content(schema = @Schema(implementation = StoreDetailResponseDto.class))}), @ApiResponse(responseCode = "404", description = "Store not found")})
    @GetMapping("/{storeId:\\d+}")
    public ResponseEntity<StoreDetailResponseDto> getStoreDetail(@Parameter(description = "ID of the store") @PathVariable Long storeId, @Parameter(description = "User latitude") @RequestParam(value = "lat", required = false) Double lat, @Parameter(description = "User longitude") @RequestParam(value = "lng", required = false) Double lng) {
        Long userId = this.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getStoreDetail(userId, storeId));
    }

    @Operation(summary = "Get store location", description = "Returns the coordinates and text of a specific store.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Store location retrieved", content = {@Content(schema = @Schema(implementation = LocationDto.class))}), @ApiResponse(responseCode = "404", description = "Store not found")})
    @GetMapping("/{storeId:\\d+}/location")
    public ResponseEntity<LocationDto> getStoreLocation(@Parameter(description = "ID of the store") @PathVariable Long storeId) {
        return ResponseEntity.ok(this.storeService.getStoreLocation(storeId));
    }

    @Operation(summary = "Save/Unsave store", description = "Toggles the 'saved' status of a store.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Toggle successful"), @ApiResponse(responseCode = "404", description = "Store not found")})
    @PostMapping("/{storeId}/save")
    public ResponseEntity<Map<String, Boolean>> saveStore(@PathVariable Long storeId) {
        Long userId = this.getCurrentUserId();
        boolean isSaved = this.storeService.toggleSave(userId, storeId);
        return ResponseEntity.ok(Map.of("is_saved", isSaved));
    }

    @Operation(summary = "Get saved stores", description = "Returns a list of all stores saved by the authenticated user.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Saved stores retrieved")})
    @GetMapping("/saved")
    public ResponseEntity<List<StoreMainPageDto>> getSavedStores() {
        Long userId = this.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getSavedStores(userId));
    }

    @Operation(summary = "Get discounted stores", description = "Returns a paginated list of stores with active discounts.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Discounted stores retrieved", content = {@Content(schema = @Schema(implementation = PaginatedResponse.class))})})
    @GetMapping("/discounted")
    public ResponseEntity<PaginatedResponse<StoreMainPageDto>> getDiscountedStores(@Parameter(description = "Search query") @RequestParam(value = "q", required = false) String q, @Parameter(description = "Page index (1-based)") @RequestParam(defaultValue = "1") int page, @Parameter(description = "Items per page") @RequestParam(defaultValue = "10") int page_size) {
        Long userId = this.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getDiscountedStores(userId, q, page, page_size));
    }

    @Operation(summary = "Get new stores", description = "Returns paginated list of stores created recently.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "New stores retrieved", content = {@Content(schema = @Schema(implementation = PaginatedResponse.class))})})
    @GetMapping("/new")
    public ResponseEntity<PaginatedResponse<StoreMainPageDto>> getNewStores(@Parameter(description = "Search query") @RequestParam(value = "q", required = false) String q, @Parameter(description = "Page index (1-based)") @RequestParam(defaultValue = "1") int page, @Parameter(description = "Items per page") @RequestParam(defaultValue = "10") int page_size) {
        Long userId = this.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getNewStores(userId, q, page, page_size));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        return null;
    }
}


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
import az.fitnest.catalog.dto.StoreRequest;
import az.fitnest.catalog.dto.StoreResponseDto;
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

@RestController
@RequestMapping(value={"/api/v1"})
@Tag(name="Stores", description="Endpoints for managing catalog stores and user interactions with stores.")
public class StoreController {
    private final StoreService storeService;

    @Operation(summary="Get catalog stores", description="Returns a paginated list of stores (same fields as /stores/closest but without distanceKm). Search (q) matches only store name and address; if q is empty, returns all stores paginated.")
    @PreAuthorize(value="isAuthenticated()")
    @SecurityRequirement(name="bearerAuth")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Stores retrieved successfully", content={@Content(schema=@Schema(implementation=StoreResponseDto.class))}), @ApiResponse(responseCode="401", description="Authentication required")})
    @GetMapping(value={"/stores"})
    public ResponseEntity<StoreResponseDto> getMarketStores(@Parameter(description="Search query (matches name or address)") @RequestParam(value="q", required=false) String q, @Parameter(description="Page index (1-based)") @RequestParam(defaultValue="1") int page, @Parameter(description="Items per page") @RequestParam(defaultValue="10") int page_size) {
        Long userId = this.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getStoreMainPage(userId, q, page, page_size));
    }

    @Operation(summary="Get closest stores", description="Returns paginated list of stores closest to provided coordinates. Returns address text and distance in kilometers.")
    @PreAuthorize(value="isAuthenticated()")
    @SecurityRequirement(name="bearerAuth")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Closest stores retrieved", content={@Content(schema=@Schema(implementation=StoreResponseDto.class))}), @ApiResponse(responseCode="401", description="Authentication required")})
    @GetMapping(value={"/stores/closest"})
    public ResponseEntity<StoreResponseDto> getClosestStores(@Parameter(description="Search query") @RequestParam(value="q", required=false) String q, @Parameter(description="Page index (1-based)") @RequestParam(defaultValue="1") int page, @Parameter(description="Items per page") @RequestParam(defaultValue="10") int page_size, @Parameter(description="User latitude") @RequestParam(value="lat", required=false) Double lat, @Parameter(description="User longitude") @RequestParam(value="lng", required=false) Double lng) {
        Long userId = this.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getClosestStores(userId, q, page, page_size, lat, lng));
    }

    @Operation(summary="Get store details", description="Retrieves full details of a specific store, including products and distance from user.")
    @PreAuthorize(value="isAuthenticated()")
    @SecurityRequirement(name="bearerAuth")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Store details retrieved successfully", content={@Content(schema=@Schema(implementation=StoreDetailResponseDto.class))}), @ApiResponse(responseCode="404", description="Store not found")})
    @GetMapping(value={"/stores/{storeId:\\d+}"})
    public ResponseEntity<StoreDetailResponseDto> getStoreDetail(@Parameter(description="ID of the store") @PathVariable Long storeId, @Parameter(description="User latitude") @RequestParam(value="lat", required=false) Double lat, @Parameter(description="User longitude") @RequestParam(value="lng", required=false) Double lng) {
        Long userId = this.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getStoreDetail(userId, storeId));
    }

    @Operation(summary="Save/Unsave store", description="Toggles the 'saved' status of a store for the current user.")
    @PreAuthorize(value="isAuthenticated()")
    @SecurityRequirement(name="bearerAuth")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Toggle successful", content={@Content(examples={@ExampleObject(value="{\"is_saved\": true}")})}), @ApiResponse(responseCode="404", description="Store not found")})
    @PostMapping(value={"/stores/{storeId}/save"})
    public ResponseEntity<Map<String, Boolean>> saveStore(@PathVariable Long storeId) {
        Long userId = this.getCurrentUserId();
        boolean isSaved = this.storeService.toggleSave(userId, storeId);
        return ResponseEntity.ok(Map.of("is_saved", isSaved));
    }

    @Operation(summary="Get saved stores", description="Returns a list of all stores saved by the authenticated user.")
    @PreAuthorize(value="isAuthenticated()")
    @SecurityRequirement(name="bearerAuth")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Saved stores retrieved")})
    @GetMapping(value={"/stores/saved"})
    public ResponseEntity<List<StoreMainPageDto>> getSavedStores() {
        Long userId = this.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getSavedStores(userId));
    }

    @Operation(summary="Get discounted stores", description="Returns a paginated list of stores that currently have active discounts. Supports search with 'q'.")
    @PreAuthorize(value="isAuthenticated()")
    @SecurityRequirement(name="bearerAuth")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Discounted stores retrieved", content={@Content(schema=@Schema(implementation=StoreResponseDto.class))})})
    @GetMapping(value={"/stores/discounted"})
    public ResponseEntity<StoreResponseDto> getDiscountedStores(@Parameter(description="Search query") @RequestParam(value="q", required=false) String q, @Parameter(description="Page index (1-based)") @RequestParam(defaultValue="1") int page, @Parameter(description="Items per page") @RequestParam(defaultValue="10") int page_size) {
        Long userId = this.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getDiscountedStores(userId, q, page, page_size));
    }

    @Operation(summary="Get new stores", description="Returns paginated list of stores created within the last 30 days. Supports search with 'q'.")
    @PreAuthorize(value="isAuthenticated()")
    @SecurityRequirement(name="bearerAuth")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="New stores retrieved", content={@Content(schema=@Schema(implementation=StoreResponseDto.class))})})
    @GetMapping(value={"/stores/new"})
    public ResponseEntity<StoreResponseDto> getNewStores(@Parameter(description="Search query") @RequestParam(value="q", required=false) String q, @Parameter(description="Page index (1-based)") @RequestParam(defaultValue="1") int page, @Parameter(description="Items per page") @RequestParam(defaultValue="10") int page_size) {
        Long userId = this.getCurrentUserId();
        return ResponseEntity.ok(this.storeService.getNewStores(userId, q, page, page_size));
    }

    @Operation(summary="Create store (Admin)", description="Creates a new store. Requires ADMIN role.")
    @SecurityRequirement(name="bearerAuth")
    @PreAuthorize(value="hasRole('ADMIN')")
    @ApiResponses(value={@ApiResponse(responseCode="201", description="Store created successfully"), @ApiResponse(responseCode="400", description="Invalid store data")})
    @PostMapping(value={"/admin/stores"})
    public ResponseEntity<StoreDetailResponseDto> createStoreAdmin(@RequestBody StoreRequest request) {
        return ResponseEntity.status(201).body(this.storeService.createStore(request));
    }

    @Operation(summary="Update store (Admin)", description="Updates an existing store. Requires ADMIN role.")
    @SecurityRequirement(name="bearerAuth")
    @PreAuthorize(value="hasRole('ADMIN')")
    @ApiResponses(value={@ApiResponse(responseCode="200", description="Store updated successfully"), @ApiResponse(responseCode="404", description="Store not found")})
    @PutMapping(value={"/admin/stores/{storeId}"})
    public ResponseEntity<StoreDetailResponseDto> updateStoreAdmin(@PathVariable Long storeId, @RequestBody StoreRequest request) {
        return ResponseEntity.ok(this.storeService.updateStore(storeId, request));
    }

    @Operation(summary="Delete store (Admin)", description="Permanently deletes a store. Requires ADMIN role.")
    @SecurityRequirement(name="bearerAuth")
    @PreAuthorize(value="hasRole('ADMIN')")
    @ApiResponses(value={@ApiResponse(responseCode="204", description="Store deleted successfully")})
    @DeleteMapping(value={"/admin/stores/{storeId}"})
    public ResponseEntity<Void> deleteStoreAdmin(@PathVariable Long storeId) {
        this.storeService.deleteStore(storeId);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long)auth.getPrincipal();
        }
        return null;
    }

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }
}


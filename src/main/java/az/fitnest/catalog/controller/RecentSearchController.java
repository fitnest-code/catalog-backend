package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.RecentSearchDto;
import az.fitnest.catalog.service.impl.RecentSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/recent-searches")
@RequiredArgsConstructor
@Tag(name = "Recent Searches", description = "İstifadəçilərin son axtarış tarixçəsini idarə etmək üçün ucluqlar.")
public class RecentSearchController {

    private final RecentSearchService recentSearchService;

    @Operation(summary = "Son axtarışları əldə edin", description = "İstifadəçinin son 10 axtarışını əldə edir. Növ (GYM və ya STORE) üzrə filtrləyin.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Axtarışlar uğurla əldə edildi"),
            @ApiResponse(responseCode = "401", description = "Autentifikasiya tələb olunur")
    })
    @GetMapping
    public ResponseEntity<List<RecentSearchDto>> getRecentSearches(
            @Parameter(description = "Axtarış növü (GYM və ya STORE)", example = "GYM") 
            @RequestParam("type") String type) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(recentSearchService.getRecentSearches(userId, type.toUpperCase()));
    }

    @Operation(summary = "Xüsusi axtarışı silin", description = "İstifadəçinin keçmişindən xüsusi bir axtarış sorğusunu silir.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping
    public ResponseEntity<Void> deleteSearch(
            @Parameter(description = "Axtarış növü (GYM və ya STORE)", example = "GYM") 
            @RequestParam("type") String type,
            @Parameter(description = "Silinəcək axtarış sorğusu") 
            @RequestParam("query") String query) {
        Long userId = getCurrentUserId();
        if (userId != null) {
            recentSearchService.deleteSearch(userId, type.toUpperCase(), query);
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Bütün axtarış tarixçəsini təmizləyin", description = "Seçilmiş növ üçün bütün axtarış tarixçəsini təmizləyir.")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/all")
    public ResponseEntity<Void> clearAllSearches(
            @Parameter(description = "Axtarış növü (GYM və ya STORE)", example = "GYM") 
            @RequestParam("type") String type) {
        Long userId = getCurrentUserId();
        if (userId != null) {
            recentSearchService.clearAllSearches(userId, type.toUpperCase());
        }
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        return null;
    }
}

package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.service.impl.GymImageService;
import az.fitnest.catalog.service.impl.GymTrainerService;
import az.fitnest.catalog.service.impl.GymWriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/gyms")
@RequiredArgsConstructor
@Tag(name = "Gym Admin", description = "İdman zallarını və məşqçiləri idarə etmək üçün administrativ ucluqlar")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class GymAdminController {

    private final GymWriteService gymWriteService;
    private final GymTrainerService gymTrainerService;
    private final GymImageService gymImageService;

    @PostMapping
    @Operation(summary = "İdman zalı yaradın (Admin)", description = "Yeni idman zalı profili yaradır. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "İdman zalı uğurla yaradıldı"), @ApiResponse(responseCode = "403", description = "Kifayət qədər icazə yoxdur")})
    public ResponseEntity<Void> createGym(@Valid @RequestBody GymRequest request) {
        this.gymWriteService.createGym(request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{gymId}")
    @Operation(summary = "İdman zalını yeniləyin (Admin)", description = "Mövcud idman zalının əsas məlumatlarını yeniləyir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "İdman zalı uğurla yeniləndi"), @ApiResponse(responseCode = "404", description = "İdman zalı tapılmadı")})
    public ResponseEntity<Void> updateGym(@PathVariable Long gymId, @Valid @RequestBody GymRequest request) {
        this.gymWriteService.updateGym(gymId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{gymId}/subscriptions")
    @Operation(summary = "İdman zalı abunəliklərini yeniləyin (Admin)", description = "İdman zalı ilə əlaqəli abunəlik planlarını və üstünlüklərini yeniləyir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Abunəliklər uğurla yeniləndi"), @ApiResponse(responseCode = "404", description = "İdman zalı tapılmadı")})
    public ResponseEntity<Void> updateGymSubscriptions(@PathVariable Long gymId, @RequestBody GymSubscriptionsUpdateRequest request) {
        this.gymWriteService.updateGymSubscriptions(gymId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{gymId}")
    @Operation(summary = "İdman zalını silin (Admin)", description = "İdman zalı profilini və bütün əlaqəli məlumatları silir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "İdman zalı uğurla silindi")})
    public ResponseEntity<Void> deleteGym(@PathVariable Long gymId) {
        this.gymWriteService.deleteGym(gymId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gymId}/trainers")
    @Operation(summary = "Məşqçi əlavə edin (Admin)", description = "Xüsusi idman zalı üçün yeni məşqçi qeydiyyatdan keçirir. ADMIN rolu tələb olunur.")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Məşqçi uğurla əlavə edildi")})
    public ResponseEntity<Void> addTrainer(@PathVariable Long gymId, @RequestBody TrainerRequest request) {
        this.gymTrainerService.addTrainer(gymId, request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{gymId}/trainers/{trainerId}")
    @Operation(summary = "Məşqçini yeniləyin (Admin)", description = "Məşqçi məlumatlarını yeniləyir. ADMIN rolu tələb olunur.")
    public ResponseEntity<Void> updateTrainer(@PathVariable Long gymId, @PathVariable Long trainerId, @RequestBody TrainerRequest request) {
        this.gymTrainerService.updateTrainer(gymId, trainerId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{gymId}/trainers/{trainerId}")
    @Operation(summary = "Məşqçini silin (Admin)", description = "Məşqçi profilini idman zalından silir. ADMIN rolu tələb olunur.")
    public ResponseEntity<Void> deleteTrainer(@PathVariable Long gymId, @PathVariable Long trainerId) {
        this.gymTrainerService.deleteTrainer(gymId, trainerId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{gymId}/logo")
    @Operation(summary = "Loqonu yeniləyin (Admin)", description = "İdman zalı üçün yeni loqo yükləyir. Köhnəsi varsa, əvəz edir.")
    public ResponseEntity<Void> updateGymLogo(@PathVariable Long gymId, @RequestParam("file") MultipartFile file) {
        this.gymWriteService.updateLogo(gymId, file);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{gymId}/logo")
    @Operation(summary = "Loqonu silin (Admin)", description = "İdman zalının loqo şəklini silir. ADMIN rolu tələb olunur.")
    public ResponseEntity<Void> deleteGymLogo(@PathVariable Long gymId) {
        this.gymImageService.deleteLogoUrl(gymId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{gymId}/cover")
    @Operation(summary = "Üz qabığı şəklini yeniləyin (Admin)", description = "İdman zalı üçün yeni üz qabığı şəkli yükləyir. Köhnəsi varsa, əvəz edir.")
    public ResponseEntity<Void> updateGymCover(@PathVariable Long gymId, @RequestParam("file") MultipartFile file) {
        this.gymWriteService.updateCoverImage(gymId, file);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{gymId}/cover")
    @Operation(summary = "Üz qabığı şəklini silin (Admin)", description = "İdman zalının üz qabığı şəklini silir. ADMIN rolu tələb olunur.")
    public ResponseEntity<Void> deleteGymCover(@PathVariable Long gymId) {
        this.gymImageService.deleteCoverImageUrl(gymId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{gymId}/rooms/{roomName}/images", consumes = {"multipart/form-data"})
    @Operation(summary = "Otaq şəkli yükləyin (Admin)", description = "İdman zalı daxilindəki bir otaqla əlaqəli interyer şəkli yükləyir. ADMIN rolu tələb olunur.")
    public ResponseEntity<GymImageDto> uploadRoomImage(@PathVariable Long gymId, @PathVariable String roomName, @RequestParam(value = "file") MultipartFile file) {
        return ResponseEntity.ok(this.gymImageService.uploadRoomImage(gymId, roomName, file));
    }

    @DeleteMapping("/{gymId}/rooms/images/{imageId}")
    @Operation(summary = "Otaq şəklini silin (Admin)", description = "İdman zalı profilindən xüsusi interyer şəklini silir. ADMIN rolu tələb olunur.")
    public ResponseEntity<Void> deleteRoomImage(@PathVariable Long gymId, @PathVariable Long imageId) {
        this.gymImageService.deleteRoomImage(gymId, imageId);
        return ResponseEntity.noContent().build();
    }
}

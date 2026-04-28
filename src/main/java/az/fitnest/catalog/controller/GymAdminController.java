package az.fitnest.catalog.controller;

import az.fitnest.catalog.dto.GymRequest;
import az.fitnest.catalog.dto.GymSubscriptionBenefitsUpdateRequest;
import az.fitnest.catalog.dto.GymReviewDto;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.TrainerRequest;
import az.fitnest.catalog.service.impl.GymWriteService;
import az.fitnest.catalog.service.impl.GymReviewService;
import az.fitnest.catalog.service.impl.GymTrainerService;
import az.fitnest.catalog.service.impl.GymReadService;
import az.fitnest.catalog.dto.GymEntranceHistoryAdminResponse;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/gyms")
@RequiredArgsConstructor
@Tag(name = "Gym Admin", description = "İdman zallarını idarə etmək üçün administrativ ucluqlar. Bu ucluqlar yalnız ADMIN və SUPER_ADMIN rollarına malik istifadəçilər tərəfindən istifadə edilə bilər.")
@SecurityRequirement(name = "bearerAuth")
public class GymAdminController {

    private final GymWriteService gymWriteService;
    private final GymReviewService gymReviewService;
    private final GymTrainerService gymTrainerService;
    private final GymReadService gymReadService;

    @Operation(summary = "Yeni idman zalı yaradın", description = "Sistemə yeni idman zalı əlavə edir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Void> createGym(@Valid @RequestBody GymRequest request) {
        gymWriteService.createGym(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "İdman zalını yeniləyin", description = "Mövcud idman zalının məlumatlarını yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateGym(@PathVariable Long id, @Valid @RequestBody GymRequest request) {
        gymWriteService.updateGym(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İdman zalı üçün mövcud abunəlik planlarını aktivləşdirin", description = "İdman zalı üçün göstərilən abunəlik planlarını aktivləşdirir. Mövcud planların üstünlükləri qorunur, siyahıda olmayanlar isə silinir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/subscriptions/enable")
    public ResponseEntity<Void> enableGymSubscription(
            @PathVariable("id") Long gymId,
            @RequestParam("subscriptionId") Long subscriptionId) {
        gymWriteService.enableGymSubscription(gymId, subscriptionId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Aktivləşdirilmiş abunəlik üçün üstünlükləri yeniləyin", description = "İdman zalı üçün artıq aktivləşdirilmiş olan müəyyən bir abunəliyin üstünlüklərini (benefits) yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/subscriptions/{planId}/benefits")
    public ResponseEntity<Void> updateGymSubscriptionBenefits(@PathVariable Long id, @PathVariable Long planId,
            @Valid @RequestBody GymSubscriptionBenefitsUpdateRequest request) {
        gymWriteService.updateGymSubscriptionBenefits(id, planId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İdman zalını silin", description = "İdman zalını sistemdən silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGym(@PathVariable Long id) {
        gymWriteService.deleteGym(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "İdman zalına otaq şəkli əlavə edin", description = "İdman zalı üçün otaq şəkilləri yükləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/rooms", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> addRoomImages(
            @PathVariable Long id,
            @RequestParam("roomNames") java.util.List<String> roomNames,
            @RequestParam("files") java.util.List<MultipartFile> files) {
        if (roomNames == null || files == null || roomNames.size() != files.size()) {
            return ResponseEntity.badRequest().build();
        }
        gymWriteService.addRoomImages(id, roomNames, files);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İdman zalının bütün otaqlarını silin", description = "İdman zalı üçün bütün otaqları və onların şəkillərini silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/rooms")
    public ResponseEntity<Void> deleteAllGymRooms(@PathVariable Long id) {
        gymWriteService.deleteAllGymRooms(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "İdman zalının müəyyən bir otağını silin", description = "İdman zalı üçün göstərilən otağı və onun şəkillərini silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/rooms/{roomId}")
    public ResponseEntity<Void> deleteGymRoomById(@PathVariable Long id, @PathVariable Long roomId) {
        gymWriteService.deleteGymRoomById(id, roomId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "İdman zalının müəyyən bir otaq şəklini silin", description = "İdman zalının otaqlarına aid olan müəyyən bir şəkli silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/rooms/images/{imageId}")
    public ResponseEntity<Void> deleteRoomImageById(@PathVariable Long id, @PathVariable Long imageId) {
        gymWriteService.deleteRoomImageById(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "İdman zalı üz qabığı şəklini yeniləyin", description = "İdman zalı üçün əsas profil şəklini yükləyir və ya yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateCoverImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        gymWriteService.updateCoverImage(id, file);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Bütün idman zallarını silin (Kritik)", description = "Sistemdəki BÜTÜN idman zallarını silir. Bu əməliyyat üçün SUPER_ADMIN rolu tələb olunur və bu hərəkət geri qaytarıla bilməz.")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAllGyms() {
        gymWriteService.deleteAllGyms();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mövcud olmayan və ya təsdiqlənməmiş rəyi təsdiqləyin", description = "İdman zalına verilmiş rəyi təsdiqləyir və ümumi reytinqə daxil edir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reviews/{reviewId}/approve")
    public ResponseEntity<Void> approveReview(@PathVariable Long reviewId) {
        gymReviewService.approveReview(reviewId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Rəyi rədd edin", description = "İdman zalına verilmiş rəyi rədd edir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reviews/{reviewId}/reject")
    public ResponseEntity<Void> rejectReview(@PathVariable Long reviewId) {
        gymReviewService.rejectReview(reviewId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Təsdiq gözləyən rəyləri alın", description = "Sistemdə təsdiq gözləyən (PENDING) rəylərin siyahısını gətirir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/reviews/pending")
    public ResponseEntity<PaginatedResponse<GymReviewDto>> getPendingReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(gymReviewService.getPendingReviews(page, pageSize));
    }

    @Operation(summary = "Bütün abunəlikləri silin", description = "İdman zalı üçün bütün abunəlikləri silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/subscriptions")
    public ResponseEntity<Void> deleteAllGymSubscriptions(@PathVariable("id") Long gymId) {
        gymWriteService.deleteAllGymSubscriptions(gymId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Abunəliyi silin", description = "İdman zalı üçün müəyyən bir abunəliyi silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/subscriptions/{subscriptionId}")
    public ResponseEntity<Void> deleteGymSubscriptionById(@PathVariable("id") Long gymId,
            @PathVariable("subscriptionId") Long subscriptionId) {
        gymWriteService.deleteGymSubscriptionById(gymId, subscriptionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "İdman zalı üçün təlimçi yaradın", description = "İdman zalına yeni təlimçi əlavə edir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/trainers")
    public ResponseEntity<Void> addTrainer(@PathVariable("id") Long gymId, @Valid @RequestBody TrainerRequest request) {
        gymTrainerService.addTrainer(gymId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Təlimçini yeniləyin", description = "İdman zalına aid təlimçinin məlumatlarını yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/trainers/{trainerId}")
    public ResponseEntity<Void> updateTrainer(@PathVariable("id") Long gymId, @PathVariable("trainerId") Long trainerId,
            @Valid @RequestBody TrainerRequest request) {
        gymTrainerService.updateTrainer(gymId, trainerId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Təlimçini silin", description = "İdman zalına aid təlimçini silir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}/trainers/{trainerId}")
    public ResponseEntity<Void> deleteTrainer(@PathVariable("id") Long gymId,
            @PathVariable("trainerId") Long trainerId) {
        gymTrainerService.deleteTrainer(gymId, trainerId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Təlimçinin şəklini yeniləyin", description = "İdman zalına aid təlimçi üçün profil şəklini yükləyir və ya yeniləyir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/trainers/{trainerId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateTrainerPhoto(
            @PathVariable("id") Long gymId,
            @PathVariable("trainerId") Long trainerId,
            @RequestParam("file") MultipartFile file) {
        gymTrainerService.updateTrainerPhoto(gymId, trainerId, file);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "İdman zalı giriş tarixçəsini alın", description = "İdman zalı üçün bütün giriş cəhdlərinin (uğurlu/uğursuz) siyahısını gətirir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<GymEntranceHistoryAdminResponse>> getGymEntranceHistory(@PathVariable("id") Long gymId) {
        return ResponseEntity.ok(gymReadService.getGymEntranceHistory(gymId));
    }

    @Operation(summary = "İdman zallarını siyahısını alın", description = "İdman zallarının adını, ünvanını, sahibini və statusunu qaytarır. Axtarış və sıralama dəstəklənir.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<PaginatedResponse<az.fitnest.catalog.dto.AdminGymResponse>> getAllGyms(
            @io.swagger.v3.oas.annotations.Parameter(description = "Zal adı, ünvan və ya şəhər üzrə axtarış") @RequestParam(required = false) String query,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sıralama qaydası. Dəyərlər: "
                    + "name_asc - Ad : A-Z, "
                    + "name_desc - Ad : Z-A, "
                    + "address_asc - Şəhər + Ünvan (A-Z), "
                    + "newest - Yeni əlavə edilmiş (son 1 həftə), "
                    + "deactivated - Deaktiv zallar", schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {
                            "name_asc", "name_desc", "address_asc", "newest", "deactivated"
                    })) @RequestParam(required = false) String sort,
            @io.swagger.v3.oas.annotations.Parameter(description = "Səhifə nömrəsi (1-dən başlayır)", example = "1") @RequestParam(defaultValue = "1") int page,
            @io.swagger.v3.oas.annotations.Parameter(description = "Hər səhifədəki elementlərin sayı", example = "10") @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(gymReadService.getAllGymsAdmin(query, sort, page, pageSize));
    }

    @Operation(summary = "İstifadəçinin QR skan tarixçəsini alın", description = "Müəyyən bir istifadəçinin bütün QR skan cəhdlərinin (uğurlu/uğursuz) siyahısını, zal adlarını və platforma məlumatlarını gətirir. ADMIN rolu tələb olunur.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/qr-history")
    public ResponseEntity<List<az.fitnest.catalog.dto.AdminQrScanHistoryResponse>> getUserQrScanHistory(
            @PathVariable Long userId,
            @io.swagger.v3.oas.annotations.Parameter(description = "Zal adı üzrə axtarış") @RequestParam(required = false) String query,
            @io.swagger.v3.oas.annotations.Parameter(description = "Sıralama qaydası. Dəyərlər: "
                    + "gymName_asc - Zal adı : A-Z, "
                    + "gymName_desc - Zal adı : Z-A, "
                    + "date_asc - Tarix : Köhnə -> Yeni, "
                    + "date_desc - Tarix : Yeni -> Köhnə, "
                    + "status_asc - Status : A-Z, "
                    + "status_desc - Status : Z-A, "
                    + "platform_asc - Platforma : A-Z, "
                    + "platform_desc - Platforma : Z-A", schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {
                            "gymName_asc", "gymName_desc", "date_asc", "date_desc",
                            "status_asc", "status_desc", "platform_asc", "platform_desc"
                    })) @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(gymReadService.getUserQrScanHistoryAdmin(userId, query, sort));
    }
    @Operation(summary = "Step 1: Yeni idman zalı yaradın (DRAFT)", description = "Sistemə yeni idman zalı layihəsini əlavə edir.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/step1")
    public ResponseEntity<az.fitnest.catalog.dto.GymCreateStep1Response> createGymStep1(@Valid @RequestBody az.fitnest.catalog.dto.GymCreateStep1Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gymWriteService.createGymStep1(request));
    }

    @Operation(summary = "Step 2: İdman zalının iş saatlarını qeyd edin", description = "İdman zalının iş saatlarını qeyd edir.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/step2")
    public ResponseEntity<Void> createGymStep2(@PathVariable Long id, @Valid @RequestBody az.fitnest.catalog.dto.GymCreateStep2Request request) {
        gymWriteService.createGymStep2(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Step 3: İdman zalının koordinatlarını qeyd edin", description = "İdman zalının ünvanını və koordinatlarını qeyd edir.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/step3")
    public ResponseEntity<Void> createGymStep3(@PathVariable Long id, @Valid @RequestBody az.fitnest.catalog.dto.GymCreateStep3Request request) {
        gymWriteService.createGymStep3(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Step 4: İdman zalına şəkillər əlavə edin", description = "İdman zalı üçün üz qabığı və otaq şəkilləri yükləyir.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/step4", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createGymStep4(
            @PathVariable Long id,
            @RequestParam("coverPhoto") MultipartFile coverPhoto,
            @RequestParam(value = "roomNames", required = false) java.util.List<String> roomNames,
            @RequestParam(value = "roomPhotos", required = false) java.util.List<MultipartFile> roomPhotos) {
        gymWriteService.createGymStep4(id, coverPhoto, roomNames, roomPhotos);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Step 5: İdman zalına təlimçi əlavə edin", description = "İdman zalına yeni təlimçi əlavə edir.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/step5")
    public ResponseEntity<Void> createGymStep5(@PathVariable Long id, @Valid @RequestBody TrainerRequest request) {
        gymTrainerService.addTrainer(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Step 6: İdman zalı üçün abunəlik və xidmətləri aktivləşdirin", description = "İdman zalı üçün abunəlikləri və dəstəklənən xidmətləri əlavə edir.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/step6")
    public ResponseEntity<Void> createGymStep6(@PathVariable Long id, @Valid @RequestBody az.fitnest.catalog.dto.GymCreateStep6Request request) {
        gymWriteService.createGymStep6(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Step 7: İdman zalı üçün admin yaradın və aktivləşdirin", description = "İdman zalı üçün admin yaradır və idman zalını ACTIVE edir.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/step7")
    public ResponseEntity<Void> createGymStep7(@PathVariable Long id, @Valid @RequestBody az.fitnest.catalog.dto.GymCreateStep7Request request) {
        gymWriteService.createGymStep7(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Dəstəklənən xidmət əlavə edin", description = "Sistemə yeni dəstəklənən xidmət əlavə edir.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/services")
    public ResponseEntity<Void> createSupportedService(@Valid @RequestBody az.fitnest.catalog.dto.SupportedServiceRequest request) {
        gymWriteService.createSupportedService(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    @Operation(summary = "Dəstəklənən xidmətləri siyahılayın", description = "Sistemdəki bütün dəstəklənən xidmətləri qaytarır.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/services")
    public ResponseEntity<java.util.List<az.fitnest.catalog.dto.SupportedServiceResponse>> getSupportedServices() {
        return ResponseEntity.ok(gymReadService.getAllSupportedServices());
    }

    @Operation(summary = "Dəstəklənən xidməti silin", description = "ID-yə görə dəstəklənən xidməti silir.")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deleteSupportedService(@PathVariable Long id) {
        gymWriteService.deleteSupportedService(id);
        return ResponseEntity.ok().build();
    }
}

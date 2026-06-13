package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface GymWriteService {

    void createGym(GymRequest request);

    void updateGym(Long gymId, GymRequest request);

    void enableGymSubscription(Long gymId, Long subscriptionId);

    void updateGymSubscriptionBenefits(Long gymId, Long packageId,
                                       GymSubscriptionBenefitsUpdateRequest request);

    void deleteGym(Long gymId);

    boolean toggleSave(Object principal, Long gymId);

    CheckInResponse checkIn(Object principal, Long gymId);

    void addRoomImages(Long gymId, List<String> roomNames,
                       List<MultipartFile> files);

    void deleteAllGymRooms(Long gymId);

    void deleteGymRoomById(Long gymId, Long roomId);

    void deleteRoomImageById(Long gymId, Long imageId);

    void updateRoomName(Long gymId, Long roomId, String name);

    void updateCoverImage(Long gymId, MultipartFile coverPhoto);

    void deleteAllGyms();

    void deleteGymEntranceHistory();

    void deleteAllGymSubscriptions(Long gymId);

    void deleteGymSubscriptionById(Long gymId, Long subscriptionId);

    void toggleGymReservation(Long gymId, boolean enabled);

    // ── V1 Step metodları ────────────────────────────────────────
    GymCreateStep1Response createGymStep1(GymCreateStep1Request request);

    void createGymStep2(Long id, List<String> names, List<String> surnames,
                        List<Long> professionIds, List<String> emails, List<String> phones,
                        List<MultipartFile> photos, List<String> lessonTypesPerTrainer);

    void createGymStep3(Long gymId, GymCreateStep2Request request);

    void createGymStep4(Long gymId, GymCreateStep3Request request);

    void createGymStep5(Long gymId, MultipartFile coverPhoto,
                        List<String> roomNames, List<MultipartFile> roomPhotos);

    void createGymStep6(Long gymId, GymCreateStep6Request request,
                        List<MultipartFile> serviceIcons);

    void createGymStep7(Long gymId, GymCreateStep7Request request);

    Long createGymComplete(GymCreateCompleteRequest request,
                           MultipartFile coverPhoto, List<MultipartFile> trainerPhotos,
                           List<MultipartFile> roomPhotos, List<MultipartFile> serviceIcons);

    // ── V1 Validate metodları ────────────────────────────────────
    void validateStep1(GymCreateStep1Request request);

    void validateStep2(List<String> emails, List<String> phones);

    void validateStep3(GymCreateStep2Request request);

    void validateStep4(GymCreateStep3Request request);

    void validateStep5(MultipartFile coverPhoto, List<String> roomNames,
                       List<MultipartFile> roomPhotos);

    void validateStep6(GymCreateStep6Request request,
                       List<MultipartFile> serviceIcons);

    void validateStep7(GymCreateStep7Request request);

    // ── V1 Update metodları ──────────────────────────────────────
    void updateGymInfo(Long gymId, GymInfoUpdateRequest request);

    void updateGymSubscriptions(Long gymId, GymCreateStep6Request request);

    void updateGymWorkHours(Long gymId, GymCreateStep2Request request);

    void toggleGymStatus(Long gymId, boolean enabled);

    void updateReservationStatusAdmin(Long reservationId,
                                      az.fitnest.catalog.model.enums.ReservationStatus status,
                                      String reason);

    void addLessonHourAdmin(Long gymId, LessonHourRequest request);

    void deleteLessonHourAdmin(Long lessonHourId);

    SupportedServiceResponse createSupportedService(
            SupportedServiceRequest request, MultipartFile icon);

    void deleteSupportedService(Long id);

    GeocodingResponse reverseGeocode(Double lat, Double lng);

    java.util.List<GeocodingResponse> forwardGeocode(String query);

    void addGymAdmin(Long gymId, GymAdminCreateRequest request);

    void updateGymAdmin(Long gymId, Long adminId, GymAdminUpdateRequest request);

    void deleteGymAdmin(Long gymId, Long adminId);

    // ── V2 metodları ─────────────────────────────────────────────

    GymCreateStep1Response createGymStep1V2(GymCreateStep1RequestV2 request);

    void validateStep1V2(GymCreateStep1RequestV2 request);

    void createGymStep5V2(Long gymId, MultipartFile coverPhoto,
                          List<String> roomNames, List<Long> roomCategoryIds,
                          List<MultipartFile> roomPhotos);

    void validateStep5V2(MultipartFile coverPhoto, List<String> roomNames,
                         List<Long> roomCategoryIds, List<MultipartFile> roomPhotos);

    void createGymStep6V2(Long gymId, GymCreateStep6RequestV2 request,
                          List<MultipartFile> serviceIcons);

    void validateStep6V2(GymCreateStep6RequestV2 request,
                         List<MultipartFile> serviceIcons);

    void updateGymSubscriptionsV2(Long gymId, GymCreateStep6RequestV2 request);

    void updateGymInfoV2(Long gymId, GymInfoUpdateRequestV2 request);

    Long createGymCompleteV2(GymCreateCompleteRequestV2 request,
                             MultipartFile coverPhoto, List<MultipartFile> trainerPhotos,
                             List<MultipartFile> roomPhotos, List<MultipartFile> serviceIcons);
}
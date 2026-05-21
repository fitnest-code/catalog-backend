package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface GymWriteService {
    void createGym(az.fitnest.catalog.dto.request.GymRequest request);
    void updateGym(Long id, az.fitnest.catalog.dto.request.GymRequest request);
    void updateGymInfo(Long id, az.fitnest.catalog.dto.request.GymInfoUpdateRequest request);
    void enableGymSubscription(Long gymId, Long subscriptionId);
    void updateGymSubscriptionBenefits(Long gymId, Long packageId, GymSubscriptionBenefitsUpdateRequest request);
    void deleteGym(Long gymId);
    boolean toggleSave(Object principal, Long gymId);
    CheckInResponse checkIn(Object principal, Long gymId);
    void addRoomImages(Long gymId, List<String> roomNames, List<MultipartFile> files);
    void deleteAllGymRooms(Long gymId);
    void deleteGymRoomById(Long gymId, Long roomId);
    void deleteRoomImageById(Long gymId, Long imageId);
    void updateCoverImage(Long gymId, MultipartFile coverPhoto);
    void deleteAllGyms();
    void deleteAllGymSubscriptions(Long gymId);
    void deleteGymSubscriptionById(Long gymId, Long subscriptionId);
    void toggleGymReservation(Long gymId, boolean enabled);
    SupportedServiceResponse createSupportedService(SupportedServiceRequest request);
    void deleteSupportedService(Long id);

    GymCreateStep1Response createGymStep1(GymCreateStep1Request request);
    void createGymStep2(Long id, List<String> names, List<String> surnames, List<Long> professionIds, List<String> emails, List<String> phones, List<MultipartFile> photos, List<String> lessonTypesPerTrainer);
    void createGymStep3(Long id, GymCreateStep2Request request);
    void createGymStep4(Long id, GymCreateStep3Request request);
    void createGymStep5(Long gymId, MultipartFile coverPhoto, List<String> roomNames, List<MultipartFile> roomPhotos);
    void createGymStep6(Long gymId, GymCreateStep6Request request);
    void updateGymSubscriptions(Long gymId, GymCreateStep6Request request);
    void createGymStep7(Long gymId, GymCreateStep7Request request);
    Long createGymComplete(GymCreateCompleteRequest request, MultipartFile coverPhoto,
                           List<MultipartFile> trainerPhotos, List<MultipartFile> roomPhotos);
    GeocodingResponse reverseGeocode(Double lat, Double lng);
    java.util.List<GeocodingResponse> forwardGeocode(String query);
    void toggleGymStatus(Long gymId, boolean enabled);
    void addGymAdmin(Long gymId, GymAdminCreateRequest request);
    void updateGymAdmin(Long gymId, Long adminId, GymAdminUpdateRequest request);
    void deleteGymAdmin(Long gymId, Long adminId);
    void updateReservationStatusAdmin(Long reservationId, az.fitnest.catalog.model.enums.ReservationStatus status, String reason);
    void addLessonHourAdmin(Long gymId, az.fitnest.catalog.dto.request.LessonHourRequest request);
    void deleteLessonHourAdmin(Long lessonHourId);
    void validateStep1(az.fitnest.catalog.dto.request.GymCreateStep1Request request);
    void validateStep2(List<String> emails, List<String> phones);
    void validateStep3(az.fitnest.catalog.dto.request.GymCreateStep2Request request);
    void validateStep4(az.fitnest.catalog.dto.request.GymCreateStep3Request request);
    void validateStep5(MultipartFile coverPhoto, List<String> roomNames, List<MultipartFile> roomPhotos);
    void validateStep6(az.fitnest.catalog.dto.request.GymCreateStep6Request request);
    void validateStep7(az.fitnest.catalog.dto.request.GymCreateStep7Request request);
}

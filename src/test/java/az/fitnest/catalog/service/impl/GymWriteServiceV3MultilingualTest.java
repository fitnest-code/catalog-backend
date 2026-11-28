package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.IdentityServiceGrpcClient;
import az.fitnest.catalog.client.NotificationsServiceGrpcClient;
import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.catalog.dto.request.CategoryDetailV3;
import az.fitnest.catalog.dto.request.GymAdminCreateRequest;
import az.fitnest.catalog.dto.request.GymCreateCompleteRequestV3;
import az.fitnest.catalog.dto.request.GymCreateStep1RequestV2;
import az.fitnest.catalog.dto.request.GymCreateStep1RequestV3;
import az.fitnest.catalog.dto.request.GymCreateStep6RequestV2;
import az.fitnest.catalog.dto.request.GymCreateStep6RequestV3;
import az.fitnest.catalog.dto.request.GymCreateStep6SubscriptionRequestV3;
import az.fitnest.catalog.dto.request.LocalizedServiceNameRequest;
import az.fitnest.catalog.dto.response.GymCreateStep1Response;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.GymAdminRepository;
import az.fitnest.catalog.repository.GymDescriptionRepository;
import az.fitnest.catalog.repository.GymEntranceHistoryRepository;
import az.fitnest.catalog.repository.GymImageRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.LessonTypeRepository;
import az.fitnest.catalog.repository.ReservationRepository;
import az.fitnest.catalog.repository.SavedGymRepository;
import az.fitnest.catalog.repository.SupportedServiceRepository;
import az.fitnest.catalog.repository.TrainerRepository;
import az.fitnest.catalog.repository.TrainerReservationDateRepository;
import az.fitnest.catalog.service.FileStorageService;
import az.fitnest.catalog.service.GymActivationNotifier;
import az.fitnest.catalog.service.GymQrCodeService;
import az.fitnest.catalog.service.GymTrainerService;
import az.fitnest.catalog.service.GymTranslationWriter;
import az.fitnest.catalog.service.ReverseGeocodingService;
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.service.homegym.HomeGymPackageEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GymWriteServiceV3MultilingualTest {

    @Mock private GymRepository gymRepository;
    @Mock private GymDescriptionRepository gymDescriptionRepository;
    @Mock private SavedGymRepository savedGymRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ReverseGeocodingService reverseGeocodingService;
    @Mock private FileStorageService fileStorageService;
    @Mock private OrderServiceGrpcClient orderServiceGrpcClient;
    @Mock private GymImageRepository gymImageRepository;
    @Mock private SupportedServiceRepository supportedServiceRepository;
    @Mock private IdentityServiceGrpcClient identityServiceGrpcClient;
    @Mock private TrainerRepository trainerRepository;
    @Mock private LessonTypeRepository lessonTypeRepository;
    @Mock private TrainerReservationDateRepository trainerReservationDateRepository;
    @Mock private GymAdminRepository gymAdminRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private GymTrainerService gymTrainerService;
    @Mock private GymQrCodeService gymQrCodeService;
    @Mock private GymEntranceHistoryRepository gymEntranceHistoryRepository;
    @Mock private TranslationService translationService;
    @Mock private GymTranslationWriter gymTranslationWriter;
    @Mock private Executor imageUploadExecutor;
    @Mock private NotificationsServiceGrpcClient notificationsServiceClient;
    @Mock private UserServiceGrpcClient userServiceGrpcClient;
    @Mock private GymActivationNotifier gymActivationNotifier;
    @Mock private HomeGymPackageEventPublisher homeGymPackageEventPublisher;

    private GymWriteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = spy(new GymWriteServiceImpl(
                gymRepository, gymDescriptionRepository, savedGymRepository, categoryRepository,
                reverseGeocodingService, fileStorageService, orderServiceGrpcClient, gymImageRepository,
                supportedServiceRepository, identityServiceGrpcClient, trainerRepository, lessonTypeRepository,
                trainerReservationDateRepository, gymAdminRepository, reservationRepository, gymTrainerService,
                gymQrCodeService, gymEntranceHistoryRepository, translationService, gymTranslationWriter,
                imageUploadExecutor, notificationsServiceClient, userServiceGrpcClient, gymActivationNotifier,
                homeGymPackageEventPublisher));
    }

    @Test
    void step1V3DelegatesToV2AndPersistsLocalizedDescriptions() {
        GymCreateStep1RequestV3 request = GymCreateStep1RequestV3.builder()
                .name("Gym AZ")
                .phone("0501112233")
                .description("AZ about")
                .descriptionEn("EN about")
                .descriptionRu("RU about")
                .mainCategoryDetails(List.of(new CategoryDetailV3(1L, null, "AZ main", "Main EN", "Main RU", null)))
                .subCategoryDetails(List.of(new CategoryDetailV3(2L, null, "AZ sub", "Sub EN", "Sub RU", null)))
                .build();
        doReturn(new GymCreateStep1Response(42L)).when(service).createGymStep1V2(any());

        assertEquals(42L, service.createGymStep1V3(request).gymId());

        ArgumentCaptor<GymCreateStep1RequestV2> captor = ArgumentCaptor.forClass(GymCreateStep1RequestV2.class);
        verify(service).createGymStep1V2(captor.capture());
        assertEquals("AZ about", captor.getValue().description());
        assertEquals(1L, captor.getValue().mainCategoryDetails().get(0).categoryId());
        assertEquals("AZ main", captor.getValue().mainCategoryDetails().get(0).description());
        verify(gymTranslationWriter).saveDescriptions(
                org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.eq("EN about"),
                org.mockito.ArgumentMatchers.eq("RU about"),
                org.mockito.ArgumentMatchers.argThat(details -> details.size() == 2
                        && details.get(0).descriptionEn().equals("Main EN")
                        && details.get(1).descriptionRu().equals("Sub RU")));
    }

    @Test
    void step6V3DelegatesToV2AndPersistsLocalizedServiceNames() {
        LocalizedServiceNameRequest custom = new LocalizedServiceNameRequest("Massage", "Massage EN", "Массаж");
        GymCreateStep6RequestV3 request = GymCreateStep6RequestV3.builder()
                .subscriptions(List.of(new GymCreateStep6SubscriptionRequestV3(
                        5L, 1L, 20.0, List.of(3L), List.of(custom))))
                .build();
        doNothing().when(service).createGymStep6V2(anyLong(), any(), any());

        service.createGymStep6V3(42L, request, List.of());

        ArgumentCaptor<GymCreateStep6RequestV2> captor = ArgumentCaptor.forClass(GymCreateStep6RequestV2.class);
        verify(service).createGymStep6V2(org.mockito.ArgumentMatchers.eq(42L), captor.capture(), any());
        assertEquals(List.of("Massage"), captor.getValue().subscriptions().get(0).customServices());
        verify(gymTranslationWriter).saveCustomServiceNames(
                org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.eq(List.of(custom)));
    }

    @Test
    void step1V3WithoutTranslationsStillDelegatesAndCallsWriter() {
        GymCreateStep1RequestV3 request = GymCreateStep1RequestV3.builder()
                .name("Gym AZ")
                .phone("0501112233")
                .description("AZ only")
                .mainCategoryDetails(List.of(new CategoryDetailV3(1L, null, "AZ", null, null, null)))
                .build();
        doReturn(new GymCreateStep1Response(7L)).when(service).createGymStep1V2(any());

        service.createGymStep1V3(request);

        verify(gymTranslationWriter).saveDescriptions(
                org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.argThat(details -> details.size() == 1));
    }

    @Test
    void step6V3KeepsBlankNamesForV2IconIndexingButDoesNotWriteThem() {
        LocalizedServiceNameRequest blank = new LocalizedServiceNameRequest("  ", "EN", "RU");
        GymCreateStep6RequestV3 request = GymCreateStep6RequestV3.builder()
                .subscriptions(List.of(new GymCreateStep6SubscriptionRequestV3(
                        5L, 1L, 20.0, null, java.util.Arrays.asList(blank, null))))
                .build();
        doNothing().when(service).createGymStep6V2(anyLong(), any(), any());

        service.createGymStep6V3(42L, request, List.of());

        ArgumentCaptor<GymCreateStep6RequestV2> captor = ArgumentCaptor.forClass(GymCreateStep6RequestV2.class);
        verify(service).createGymStep6V2(org.mockito.ArgumentMatchers.eq(42L), captor.capture(), any());
        assertEquals(List.of("  "), captor.getValue().subscriptions().get(0).customServices());
        verify(gymTranslationWriter).saveCustomServiceNames(
                org.mockito.ArgumentMatchers.eq(42L), org.mockito.ArgumentMatchers.eq(List.of(blank)));
    }

    @Test
    void validationMethodsDelegateToV2() {
        GymCreateStep1RequestV3 step1 = GymCreateStep1RequestV3.builder().name("Gym").build();
        GymCreateStep6RequestV3 step6 = GymCreateStep6RequestV3.builder().build();
        doNothing().when(service).validateStep1V2(any());
        doNothing().when(service).validateStep6V2(any(), any());

        service.validateStep1V3(step1);
        service.validateStep6V3(step6, List.of());

        verify(service).validateStep1V2(any(GymCreateStep1RequestV2.class));
        verify(service).validateStep6V2(any(GymCreateStep6RequestV2.class), any());
    }

    @Test
    void createCompleteV3DelegatesAndPersistsAllLocalizedValues() {
        GymCreateCompleteRequestV3 request = GymCreateCompleteRequestV3.builder()
                .name("Gym AZ")
                .phone("0501112233")
                .description("AZ")
                .descriptionEn("EN")
                .descriptionRu("RU")
                .latitude(40.0)
                .longitude(49.0)
                .mainCategoryDetails(List.of(new CategoryDetailV3(1L, null, "AZ main", "Main EN", "Main RU", null)))
                .subCategoryDetails(List.of(new CategoryDetailV3(2L, null, "AZ sub", "Sub EN", "Sub RU", null)))
                .subscriptions(List.of(new GymCreateStep6SubscriptionRequestV3(
                        5L, 1L, 20.0, null,
                        List.of(new LocalizedServiceNameRequest("Massage", "Massage EN", "Массаж")))))
                .admins(List.of(new GymAdminCreateRequest(
                        "Admin", "User", "0501112233", "a@b.az", "pass123", "Super admin")))
                .build();
        doReturn(99L).when(service).createGymCompleteV2(any(), any(), any(), any(), any(), any(), any());

        assertEquals(99L, service.createGymCompleteV3(request, null, null, null, null, null, null));

        verify(service).createGymCompleteV2(any(), any(), any(), any(), any(), any(), any());
        verify(gymTranslationWriter).saveDescriptions(
                org.mockito.ArgumentMatchers.eq(99L),
                org.mockito.ArgumentMatchers.eq("EN"),
                org.mockito.ArgumentMatchers.eq("RU"),
                org.mockito.ArgumentMatchers.argThat(details -> details.size() == 2));
        verify(gymTranslationWriter).saveCustomServiceNames(
                org.mockito.ArgumentMatchers.eq(99L),
                org.mockito.ArgumentMatchers.argThat(names -> names.size() == 1
                        && "Massage EN".equals(names.get(0).nameEn())));
    }

    @Test
    void updateSubscriptionsV3DelegatesAndRefreshesServiceTranslations() {
        GymCreateStep6RequestV3 request = GymCreateStep6RequestV3.builder()
                .subscriptions(List.of(new GymCreateStep6SubscriptionRequestV3(
                        5L, 1L, 20.0, null,
                        List.of(new LocalizedServiceNameRequest("Sauna", "Sauna EN", "Сауна")))))
                .build();
        doNothing().when(service).updateGymSubscriptionsV2(anyLong(), any());

        service.updateGymSubscriptionsV3(42L, request);

        verify(service).updateGymSubscriptionsV2(org.mockito.ArgumentMatchers.eq(42L),
                any(GymCreateStep6RequestV2.class));
        verify(gymTranslationWriter).saveCustomServiceNames(
                org.mockito.ArgumentMatchers.eq(42L),
                org.mockito.ArgumentMatchers.argThat(names -> names.size() == 1));
    }
}

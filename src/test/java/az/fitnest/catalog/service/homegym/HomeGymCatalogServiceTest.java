package az.fitnest.catalog.service.homegym;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.response.HomeGymGymResponse;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.util.AzerbaijanLocations;
import az.fitnest.order.grpc.GetHomeGymContextResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * R1 catalogue listing: order-backend context, paging normalization, plain vs closest query,
 * eligibility filtering, address/distance mapping and available hours.
 */
class HomeGymCatalogServiceTest {

    private static final Long SUBSCRIPTION_ID = 5L;
    private static final Long USER_ID = 9L;
    private static final Long GOLD = 1203L;
    private static final List<Long> ELIGIBLE = List.of(1201L, 1202L, 1203L);

    private GymRepository gymRepository;
    private HomeGymRuleService ruleService;
    private OrderServiceGrpcClient orderServiceGrpcClient;
    private HomeGymCatalogService service;

    @BeforeEach
    void setUp() {
        gymRepository = mock(GymRepository.class);
        ruleService = mock(HomeGymRuleService.class);
        orderServiceGrpcClient = mock(OrderServiceGrpcClient.class);
        service = new HomeGymCatalogService(gymRepository, ruleService, orderServiceGrpcClient);
        when(orderServiceGrpcClient.getHomeGymContext(USER_ID, SUBSCRIPTION_ID))
                .thenReturn(context(GOLD));
        when(ruleService.packageContext(GOLD))
                .thenReturn(new HomeGymRuleService.PackageContext(GOLD, "Gold", 3, ELIGIBLE));
        when(ruleService.eligiblePackageIds(GOLD)).thenReturn(ELIGIBLE);
    }

    @Test
    void aMissingSubscriptionIsReportedAsNotFound() {
        when(orderServiceGrpcClient.getHomeGymContext(USER_ID, SUBSCRIPTION_ID))
                .thenReturn(context(0L, false));

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class,
                () -> service.getGyms(USER_ID, SUBSCRIPTION_ID, null, null, 1, 10, null, null));

        assertEquals("SUBSCRIPTION_NOT_FOUND", error.getErrorCode());
        verify(gymRepository, never()).findHomeGymIds(any(), any(), any(), any());
    }

    @Test
    void pagingIsClampedToOnePageAndAHundredItems() {
        when(gymRepository.findHomeGymIds(isNull(), isNull(), eq(ELIGIBLE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));

        PaginatedResponse<HomeGymGymResponse> response =
                service.getGyms(USER_ID, SUBSCRIPTION_ID, null, null, 0, 0, null, null);

        assertEquals(1, response.page());
        assertEquals(1, response.pageSize());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(gymRepository).findHomeGymIds(isNull(), isNull(), eq(ELIGIBLE), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(1, captor.getValue().getPageSize());
    }

    @Test
    void pageSizeIsCappedAtOneHundred() {
        when(gymRepository.findHomeGymIds(isNull(), isNull(), eq(ELIGIBLE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        PaginatedResponse<HomeGymGymResponse> response =
                service.getGyms(USER_ID, SUBSCRIPTION_ID, null, null, 2, 500, null, null);

        assertEquals(2, response.page());
        assertEquals(100, response.pageSize());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(gymRepository).findHomeGymIds(isNull(), isNull(), eq(ELIGIBLE), captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(100, captor.getValue().getPageSize());
    }

    @Test
    void theSearchTermIsTrimmedAndBlankBecomesNull() {
        when(gymRepository.findHomeGymIds(any(), isNull(), eq(ELIGIBLE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        service.getGyms(USER_ID, SUBSCRIPTION_ID, "  FitZone  ", null, 1, 10, null, null);
        service.getGyms(USER_ID, SUBSCRIPTION_ID, "   ", null, 1, 10, null, null);

        verify(gymRepository).findHomeGymIds(eq("FitZone"), isNull(), eq(ELIGIBLE), any(Pageable.class));
        verify(gymRepository).findHomeGymIds(isNull(), isNull(), eq(ELIGIBLE), any(Pageable.class));
    }

    @Test
    void theCategoryFilterIsPassedToThePlainQuery() {
        when(gymRepository.findHomeGymIds(isNull(), eq(7L), eq(ELIGIBLE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        service.getGyms(USER_ID, SUBSCRIPTION_ID, null, 7L, 1, 10, null, null);

        verify(gymRepository).findHomeGymIds(isNull(), eq(7L), eq(ELIGIBLE), any(Pageable.class));
    }

    @Test
    void theCategoryFilterIsPassedToTheClosestQuery() {
        when(gymRepository.findClosestHomeGymIds(isNull(), eq(7L), eq(ELIGIBLE), eq(40.0), eq(49.0),
                any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        service.getGyms(USER_ID, SUBSCRIPTION_ID, null, 7L, 1, 10, 40.0, 49.0);

        verify(gymRepository).findClosestHomeGymIds(isNull(), eq(7L), eq(ELIGIBLE), eq(40.0), eq(49.0),
                any(Pageable.class));
    }

    @Test
    void theMainCategoryIsMappedToTheItem() {
        gymPage(List.of(1L), 1);
        Gym gym = gym(1L, "FitZone", address(40.0, 49.0));
        gym.getMainCategories().add(category(7L, "Fitness", "https://cdn/fitness.png"));
        gym.getSubCategories().add(category(8L, "CrossFit", "https://cdn/crossfit.png"));
        when(gymRepository.findWithListDetailsByIdIn(List.of(1L))).thenReturn(List.of(gym));
        when(ruleService.isHomeEligible(gym, GOLD)).thenReturn(true);

        HomeGymGymResponse item = service
                .getGyms(USER_ID, SUBSCRIPTION_ID, null, null, 1, 10, null, null)
                .items().get(0);

        assertEquals(7L, item.categoryId());
        assertEquals("Fitness", item.categoryName());
        assertEquals("https://cdn/fitness.png", item.categoryIconUrl());
    }

    @Test
    void aSubCategoryIsUsedWhenThereIsNoMainCategory() {
        gymPage(List.of(1L), 1);
        Gym gym = gym(1L, "FitZone", address(40.0, 49.0));
        gym.getSubCategories().add(category(8L, "CrossFit", "https://cdn/crossfit.png"));
        when(gymRepository.findWithListDetailsByIdIn(List.of(1L))).thenReturn(List.of(gym));
        when(ruleService.isHomeEligible(gym, GOLD)).thenReturn(true);

        HomeGymGymResponse item = service
                .getGyms(USER_ID, SUBSCRIPTION_ID, null, null, 1, 10, null, null)
                .items().get(0);

        assertEquals(8L, item.categoryId());
        assertEquals("CrossFit", item.categoryName());
        assertEquals("https://cdn/crossfit.png", item.categoryIconUrl());
    }

    @Test
    void aGymWithoutCategoriesHasNoCategoryFields() {
        gymPage(List.of(1L), 1);
        Gym gym = gym(1L, "FitZone", address(40.0, 49.0));
        when(gymRepository.findWithListDetailsByIdIn(List.of(1L))).thenReturn(List.of(gym));
        when(ruleService.isHomeEligible(gym, GOLD)).thenReturn(true);

        HomeGymGymResponse item = service
                .getGyms(USER_ID, SUBSCRIPTION_ID, null, null, 1, 10, null, null)
                .items().get(0);

        assertNull(item.categoryId());
        assertNull(item.categoryName());
        assertNull(item.categoryIconUrl());
    }

    @Test
    void coordinatesSwitchTheQueryToTheClosestGyms() {
        when(gymRepository.findClosestHomeGymIds(eq("FitZone"), isNull(), eq(ELIGIBLE), eq(40.0), eq(49.0),
                any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        service.getGyms(USER_ID, SUBSCRIPTION_ID, "FitZone", null, 1, 10, 40.0, 49.0);

        verify(gymRepository).findClosestHomeGymIds(eq("FitZone"), isNull(), eq(ELIGIBLE), eq(40.0), eq(49.0),
                any(Pageable.class));
        verify(gymRepository, never()).findHomeGymIds(any(), any(), any(), any());
    }

    @Test
    void aSingleCoordinateFallsBackToThePlainQuery() {
        when(gymRepository.findHomeGymIds(isNull(), isNull(), eq(ELIGIBLE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        service.getGyms(USER_ID, SUBSCRIPTION_ID, null, null, 1, 10, 40.0, null);

        verify(gymRepository).findHomeGymIds(isNull(), isNull(), eq(ELIGIBLE), any(Pageable.class));
        verify(gymRepository, never()).findClosestHomeGymIds(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aZeroPackageIdIsTreatedAsMissing() {
        when(orderServiceGrpcClient.getHomeGymContext(USER_ID, SUBSCRIPTION_ID)).thenReturn(context(0L));
        when(ruleService.packageContext(null))
                .thenReturn(new HomeGymRuleService.PackageContext(null, null, 0, List.of()));
        when(ruleService.eligiblePackageIds(null)).thenReturn(List.of(-1L));
        when(gymRepository.findHomeGymIds(isNull(), isNull(), eq(List.of(-1L)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        service.getGyms(USER_ID, SUBSCRIPTION_ID, null, null, 1, 10, null, null);

        verify(ruleService).packageContext(null);
        verify(gymRepository).findHomeGymIds(isNull(), isNull(), eq(List.of(-1L)), any(Pageable.class));
    }

    @Test
    void ineligibleGymsAreFilteredOutOfThePage() {
        gymPage(List.of(1L, 2L), 2);
        Gym first = gym(1L, "FitZone", address(40.0, 49.0));
        Gym second = gym(2L, "NeoGym", address(40.1, 49.0));
        when(gymRepository.findWithListDetailsByIdIn(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(ruleService.isHomeEligible(first, GOLD)).thenReturn(true);
        when(ruleService.isHomeEligible(second, GOLD)).thenReturn(false);

        PaginatedResponse<HomeGymGymResponse> response =
                service.getGyms(USER_ID, SUBSCRIPTION_ID, null, null, 1, 10, null, null);

        assertEquals(1, response.items().size());
        assertEquals("1", response.items().get(0).gymId());
        // The total still reflects the repository page, not the filtered items.
        assertEquals(2, response.total());
    }

    @Test
    void repositoryOrderIsKeptAndMissingGymsAreSkipped() {
        gymPage(List.of(3L, 1L, 2L), 3);
        Gym first = gym(1L, "One", address(null, null));
        Gym second = gym(2L, "Two", address(null, null));
        when(gymRepository.findWithListDetailsByIdIn(List.of(3L, 1L, 2L))).thenReturn(List.of(first, second));
        when(ruleService.isHomeEligible(any(Gym.class), eq(GOLD))).thenReturn(true);

        PaginatedResponse<HomeGymGymResponse> response =
                service.getGyms(USER_ID, SUBSCRIPTION_ID, null, null, 1, 10, null, null);

        assertEquals(List.of("1", "2"),
                response.items().stream().map(HomeGymGymResponse::gymId).toList());
    }

    @Test
    void addressAndDistanceAreMappedForTheList() {
        gymPage(List.of(1L), 1);
        Gym gym = gym(1L, "FitZone", address(40.1, 49.0));
        when(gymRepository.findWithListDetailsByIdIn(List.of(1L))).thenReturn(List.of(gym));
        when(ruleService.isHomeEligible(gym, GOLD)).thenReturn(true);
        when(ruleService.availableHours(eq(gym), any(HomeGymRuleService.PackageContext.class)))
                .thenReturn("09:00-18:00");

        PaginatedResponse<HomeGymGymResponse> response =
                service.getGyms(USER_ID, SUBSCRIPTION_ID, null, null, 1, 10, 40.0, 49.0);

        HomeGymGymResponse item = response.items().get(0);
        assertEquals("FitZone", item.name());
        assertEquals("https://cdn/fitzone.png", item.coverImageUrl());
        assertEquals(AzerbaijanLocations.canonical("Ganja"), item.city());
        assertEquals("28 May street", item.location());
        assertEquals(40.1, item.latitude());
        assertEquals(49.0, item.longitude());
        assertEquals(11.1, item.distanceKm());
        assertEquals(4.5, item.rating());
        assertEquals("09:00-18:00", item.availableHours());
    }

    @Test
    void aGymWithoutAnAddressHasNoLocationFields() {
        gymPage(List.of(1L), 1);
        Gym gym = gym(1L, "FitZone", null);
        when(gymRepository.findWithListDetailsByIdIn(List.of(1L))).thenReturn(List.of(gym));
        when(ruleService.isHomeEligible(gym, GOLD)).thenReturn(true);

        HomeGymGymResponse item = service
                .getGyms(USER_ID, SUBSCRIPTION_ID, null, null, 1, 10, 40.0, 49.0)
                .items().get(0);

        assertNull(item.city());
        assertNull(item.location());
        assertNull(item.latitude());
        assertNull(item.longitude());
        assertNull(item.distanceKm());
        assertNull(item.availableHours());
    }

    @Test
    void distanceIsOnlyComputedWhenBothSidesHaveCoordinates() {
        gymPage(List.of(1L), 1);
        Gym gym = gym(1L, "FitZone", address(null, null));
        when(gymRepository.findWithListDetailsByIdIn(List.of(1L))).thenReturn(List.of(gym));
        when(ruleService.isHomeEligible(gym, GOLD)).thenReturn(true);

        HomeGymGymResponse item = service
                .getGyms(USER_ID, SUBSCRIPTION_ID, null, null, 1, 10, 40.0, 49.0)
                .items().get(0);

        assertEquals(AzerbaijanLocations.canonical("Ganja"), item.city());
        assertNull(item.distanceKm());
    }

    @Test
    void anEmptyPageReturnsNoItemsAndTheRepositoryTotal() {
        when(gymRepository.findHomeGymIds(isNull(), isNull(), eq(ELIGIBLE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PaginatedResponse<HomeGymGymResponse> response =
                service.getGyms(USER_ID, SUBSCRIPTION_ID, null, null, 1, 10, null, null);

        assertTrue(response.items().isEmpty());
        assertEquals(0, response.total());
        assertEquals(1, response.page());
        assertEquals(10, response.pageSize());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void gymPage(List<Long> ids, long total) {
        Page<Long> page = new PageImpl<>(ids, PageRequest.of(0, 10), total);
        when(gymRepository.findHomeGymIds(isNull(), isNull(), eq(ELIGIBLE), any(Pageable.class)))
                .thenReturn(page);
        when(gymRepository.findClosestHomeGymIds(isNull(), isNull(), eq(ELIGIBLE), any(Double.class),
                any(Double.class), any(Pageable.class))).thenReturn(page);
    }

    private static GetHomeGymContextResponse context(Long packageId) {
        return context(packageId, true);
    }

    private static GetHomeGymContextResponse context(Long packageId, boolean found) {
        return GetHomeGymContextResponse.newBuilder()
                .setFound(found)
                .setUserId(USER_ID)
                .setSubscriptionId(SUBSCRIPTION_ID)
                .setPackageId(packageId)
                .build();
    }

    private static Gym gym(Long id, String name, Address address) {
        Gym gym = Gym.builder()
                .name(name)
                .coverImageUrl("https://cdn/fitzone.png")
                .rating(4.5)
                .address(address)
                .build();
        gym.setId(id);
        return gym;
    }

    private static Category category(Long id, String name, String iconUrl) {
        Category category = Category.builder()
                .name(name)
                .iconUrl(iconUrl)
                .build();
        category.setId(id);
        return category;
    }

    private static Address address(Double latitude, Double longitude) {
        return Address.builder()
                .city("Ganja")
                .addressText("28 May street")
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }
}

package az.fitnest.catalog.controller;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.dto.response.GymPaymentReportItem;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.repository.GymEntranceHistoryRepository;
import az.fitnest.catalog.repository.GymPackagePriceRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.service.AppQrCodeService;
import az.fitnest.order.grpc.PackageNameInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GymReportControllerGymPaymentsTest {

    @Mock
    private GymEntranceHistoryRepository gymEntranceHistoryRepository;
    @Mock
    private GymRepository gymRepository;
    @Mock
    private GymPackagePriceRepository gymPackagePriceRepository;
    @Mock
    private OrderServiceGrpcClient orderServiceGrpcClient;
    @Mock
    private AppQrCodeService appQrCodeService;

    @InjectMocks
    private GymReportController controller;

    @Test
    void gymPayments_usesEntryPriceTierNotMemberPackage() {
        // Platinum member visited bronze-priced gym (3.50) — badge must be Bronze.
        when(gymEntranceHistoryRepository.getGymPaymentsReport(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 1204L, 1L, 3.50}));

        GymSubscription bronze = new GymSubscription();
        bronze.setPackageId(1201L);
        bronze.setDailyPrice(3.50);

        Gym gym = new Gym();
        gym.setId(10L);
        gym.setName("Puls Fitness");
        gym.setSubscriptions(Set.of(bronze));

        when(gymRepository.findWithListDetailsByIdIn(anyList())).thenReturn(List.of(gym));
        when(gymPackagePriceRepository
                .findByGymIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDescPackageIdAsc(anyLong(), any()))
                .thenReturn(List.of());
        when(orderServiceGrpcClient.getPackageNamesByIds(anyList())).thenReturn(List.of(
                PackageNameInfo.newBuilder().setPackageId(1201L).setName("Bronze").build(),
                PackageNameInfo.newBuilder().setPackageId(1204L).setName("Platinum").build()
        ));

        ResponseEntity<List<GymPaymentReportItem>> res = controller.getGymPaymentsReport(
                LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 11, 28, 23, 59));

        assertNotNull(res.getBody());
        assertEquals(1, res.getBody().size());
        GymPaymentReportItem item = res.getBody().get(0);
        assertEquals("Puls Fitness", item.gymName());
        assertEquals("Bronze", item.subscription());
        assertEquals(1201L, item.packageId());
        assertEquals(3.50, item.amount());
    }

    @Test
    void gymPayments_excludesZeroAmountFreeEntries() {
        // Successful scan with amount 0 (stub/free) must not appear as a payment obligation.
        when(gymEntranceHistoryRepository.getGymPaymentsReport(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{251L, 4L, 1L, 0.0}));

        GymSubscription bronze = new GymSubscription();
        bronze.setPackageId(1L);
        bronze.setDailyPrice(5.0);
        GymSubscription platinum = new GymSubscription();
        platinum.setPackageId(4L);
        platinum.setDailyPrice(5.0);

        Gym gym = new Gym();
        gym.setId(251L);
        gym.setName("Go Premium Fitness");
        gym.setSubscriptions(Set.of(bronze, platinum));

        when(gymRepository.findWithListDetailsByIdIn(anyList())).thenReturn(List.of(gym));

        ResponseEntity<List<GymPaymentReportItem>> res = controller.getGymPaymentsReport(
                LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 11, 28, 23, 59));

        assertNotNull(res.getBody());
        assertTrue(res.getBody().isEmpty());
    }

    @Test
    void gymPayments_prefersIncludedPackagePriceWhenDailyPricesCollide() {
        // All subscription daily prices are 5; only Silver is included @ 5 in package prices.
        when(gymEntranceHistoryRepository.getGymPaymentsReport(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{251L, 4L, 1L, 5.0}));

        GymSubscription bronze = new GymSubscription();
        bronze.setPackageId(1L);
        bronze.setDailyPrice(5.0);
        GymSubscription silver = new GymSubscription();
        silver.setPackageId(2L);
        silver.setDailyPrice(5.0);
        GymSubscription platinum = new GymSubscription();
        platinum.setPackageId(4L);
        platinum.setDailyPrice(5.0);

        Gym gym = new Gym();
        gym.setId(251L);
        gym.setName("Any Silver Gym");
        gym.setSubscriptions(Set.of(bronze, silver, platinum));

        az.fitnest.catalog.model.entity.GymPackagePrice silverPrice =
                az.fitnest.catalog.model.entity.GymPackagePrice.builder()
                        .packageId(2L)
                        .included(true)
                        .singlePrice(5.0)
                        .unlimitedPrice(50.0)
                        .effectiveFrom(java.time.LocalDate.of(2026, 11, 1))
                        .build();

        when(gymRepository.findWithListDetailsByIdIn(anyList())).thenReturn(List.of(gym));
        when(gymPackagePriceRepository
                .findByGymIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDescPackageIdAsc(anyLong(), any()))
                .thenReturn(List.of(silverPrice));
        when(orderServiceGrpcClient.getPackageNamesByIds(anyList())).thenReturn(List.of(
                PackageNameInfo.newBuilder().setPackageId(1L).setName("Bronze").build(),
                PackageNameInfo.newBuilder().setPackageId(2L).setName("Silver").build(),
                PackageNameInfo.newBuilder().setPackageId(4L).setName("Platinum").build()
        ));

        ResponseEntity<List<GymPaymentReportItem>> res = controller.getGymPaymentsReport(
                LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 11, 28, 23, 59));

        assertEquals(1, res.getBody().size());
        assertEquals("Silver", res.getBody().get(0).subscription());
        assertEquals(2L, res.getBody().get(0).packageId());
    }
}

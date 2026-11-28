package az.fitnest.catalog.controller;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.dto.response.GymPaymentReportItem;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.repository.GymEntranceHistoryRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GymReportControllerGymPaymentsTest {

    @Mock
    private GymEntranceHistoryRepository gymEntranceHistoryRepository;
    @Mock
    private GymRepository gymRepository;
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
}

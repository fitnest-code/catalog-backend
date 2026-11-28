package az.fitnest.catalog.service.dashboard;

import az.fitnest.catalog.dto.dashboard.DashboardGymObligationsResponse;
import az.fitnest.catalog.dto.dashboard.DashboardTopGymsResponse;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.repository.GymEntranceHistoryRepository;
import az.fitnest.catalog.repository.GymRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardGymServiceTest {

    @Mock
    private GymEntranceHistoryRepository gymEntranceHistoryRepository;
    @Mock
    private GymRepository gymRepository;

    @InjectMocks
    private DashboardGymService service;

    @Test
    void obligations_sumsAndResolvesNames() {
        when(gymEntranceHistoryRepository.aggregateObligationsByGym(any(), any()))
                .thenReturn(List.of(
                        new Object[]{1L, 5L, 100.0},
                        new Object[]{2L, 2L, 50.0}
                ));
        Gym g1 = new Gym();
        g1.setId(1L);
        g1.setName("Alpha");
        Gym g2 = new Gym();
        g2.setId(2L);
        g2.setName("Beta");
        when(gymRepository.findWithListDetailsByIdIn(anyList())).thenReturn(List.of(g1, g2));

        DashboardGymObligationsResponse res = service.getObligations(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertEquals(150.0, res.totalObligation());
        assertEquals(2, res.gyms().size());
        assertEquals("Alpha", res.gyms().get(0).gymName());
    }

    @Test
    void topGyms_respectsLimit() {
        when(gymEntranceHistoryRepository.aggregateObligationsByGym(any(), any()))
                .thenReturn(List.of(
                        new Object[]{1L, 5L, 100.0},
                        new Object[]{2L, 2L, 50.0},
                        new Object[]{3L, 1L, 10.0}
                ));
        when(gymRepository.findWithListDetailsByIdIn(anyList())).thenReturn(List.of());

        DashboardTopGymsResponse res = service.getTopGyms(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 2);

        assertEquals(2, res.gyms().size());
    }
}

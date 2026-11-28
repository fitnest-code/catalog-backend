package az.fitnest.catalog.service.dashboard;

import az.fitnest.catalog.dto.dashboard.DashboardGymObligationItem;
import az.fitnest.catalog.dto.dashboard.DashboardGymObligationsResponse;
import az.fitnest.catalog.dto.dashboard.DashboardTopGymItem;
import az.fitnest.catalog.dto.dashboard.DashboardTopGymsResponse;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.repository.GymEntranceHistoryRepository;
import az.fitnest.catalog.repository.GymRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardGymService {

    private static final ZoneId BAKU = ZoneId.of("Asia/Baku");

    private final GymEntranceHistoryRepository gymEntranceHistoryRepository;
    private final GymRepository gymRepository;

    public DashboardGymObligationsResponse getObligations(LocalDate dateFrom, LocalDate dateTo) {
        LocalDate[] range = resolveRange(dateFrom, dateTo);
        List<DashboardGymObligationItem> items = loadItems(range[0], range[1]);
        double total = items.stream().mapToDouble(DashboardGymObligationItem::amount).sum();
        return new DashboardGymObligationsResponse(total, items, range[0].toString(), range[1].toString());
    }

    public DashboardTopGymsResponse getTopGyms(LocalDate dateFrom, LocalDate dateTo, int limit) {
        LocalDate[] range = resolveRange(dateFrom, dateTo);
        int capped = Math.min(Math.max(limit, 1), 50);
        List<DashboardGymObligationItem> items = loadItems(range[0], range[1]);
        List<DashboardTopGymItem> top = items.stream()
                .limit(capped)
                .map(i -> new DashboardTopGymItem(i.gymId(), i.gymName(), i.qrCount(), i.amount()))
                .collect(Collectors.toList());
        return new DashboardTopGymsResponse(top, range[0].toString(), range[1].toString());
    }

    private List<DashboardGymObligationItem> loadItems(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.of(23, 59, 59));
        List<Object[]> raw = gymEntranceHistoryRepository.aggregateObligationsByGym(start, end);

        List<Long> gymIds = raw.stream()
                .map(row -> (Long) row[0])
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> names = resolveGymNames(gymIds);

        List<DashboardGymObligationItem> items = new ArrayList<>(raw.size());
        for (Object[] row : raw) {
            Long gymId = (Long) row[0];
            long count = row[1] instanceof Number n ? n.longValue() : 0L;
            double amount = row[2] instanceof Number n ? n.doubleValue() : 0.0;
            items.add(new DashboardGymObligationItem(
                    gymId,
                    names.getOrDefault(gymId, "Unknown Gym"),
                    count,
                    amount
            ));
        }
        return items;
    }

    private Map<Long, String> resolveGymNames(List<Long> gymIds) {
        Map<Long, String> names = new HashMap<>();
        if (gymIds.isEmpty()) {
            return names;
        }
        try {
            for (Gym gym : gymRepository.findWithListDetailsByIdIn(new ArrayList<>(gymIds))) {
                names.put(gym.getId(), gym.getName());
            }
        } catch (Exception e) {
            log.warn("Failed to resolve gym names for dashboard: {}", e.getMessage());
        }
        return names;
    }

    private static LocalDate[] resolveRange(LocalDate dateFrom, LocalDate dateTo) {
        LocalDate today = LocalDate.now(BAKU);
        LocalDate to = dateTo != null ? dateTo : today;
        LocalDate from = dateFrom != null ? dateFrom : to.minusDays(29);
        if (from.isAfter(to)) {
            return new LocalDate[]{to, from};
        }
        return new LocalDate[]{from, to};
    }
}

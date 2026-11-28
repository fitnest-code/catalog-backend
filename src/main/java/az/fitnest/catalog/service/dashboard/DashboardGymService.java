package az.fitnest.catalog.service.dashboard;

import az.fitnest.catalog.dto.dashboard.DashboardGymObligationItem;
import az.fitnest.catalog.dto.dashboard.DashboardGymObligationsResponse;
import az.fitnest.catalog.dto.dashboard.DashboardTopGymItem;
import az.fitnest.catalog.dto.dashboard.DashboardTopGymsResponse;
import az.fitnest.catalog.model.entity.Category;
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
import java.time.temporal.ChronoUnit;
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
        List<DashboardGymObligationItem> items = loadObligationItems(range[0], range[1]);
        double total = items.stream().mapToDouble(DashboardGymObligationItem::amount).sum();
        return new DashboardGymObligationsResponse(total, items, range[0].toString(), range[1].toString());
    }

    public DashboardTopGymsResponse getTopGyms(LocalDate dateFrom, LocalDate dateTo, int limit) {
        LocalDate[] range = resolveRange(dateFrom, dateTo);
        int capped = Math.min(Math.max(limit, 1), 50);

        LocalDateTime start = range[0].atStartOfDay();
        LocalDateTime end = range[1].atTime(LocalTime.of(23, 59, 59));
        long days = Math.max(1, ChronoUnit.DAYS.between(range[0], range[1]) + 1);
        LocalDateTime prevStart = range[0].minusDays(days).atStartOfDay();
        LocalDateTime prevEnd = range[0].minusDays(1).atTime(LocalTime.of(23, 59, 59));

        List<Object[]> current = gymEntranceHistoryRepository.aggregateTopGymsByScans(start, end);
        List<Object[]> previous = gymEntranceHistoryRepository.aggregateTopGymsByScans(prevStart, prevEnd);
        Map<Long, Long> prevScans = new HashMap<>();
        for (Object[] row : previous) {
            Long gymId = (Long) row[0];
            long count = row[1] instanceof Number n ? n.longValue() : 0L;
            prevScans.put(gymId, count);
        }

        List<Long> gymIds = current.stream()
                .map(row -> (Long) row[0])
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Gym> gyms = resolveGyms(gymIds);

        List<DashboardTopGymItem> top = new ArrayList<>();
        for (Object[] row : current) {
            if (top.size() >= capped) {
                break;
            }
            Long gymId = (Long) row[0];
            long qrCount = row[1] instanceof Number n ? n.longValue() : 0L;
            long unique = row[2] instanceof Number n ? n.longValue() : 0L;
            double amount = row[3] instanceof Number n ? n.doubleValue() : 0.0;
            Gym gym = gyms.get(gymId);
            String name = gym != null ? gym.getName() : "Unknown Gym";
            String category = resolveCategory(gym);
            Double trend = null;
            long prev = prevScans.getOrDefault(gymId, 0L);
            if (prev > 0) {
                trend = ((qrCount - prev) * 100.0) / prev;
            }
            top.add(new DashboardTopGymItem(
                    gymId, name, category, "", qrCount, unique, amount, trend));
        }
        return new DashboardTopGymsResponse(top, range[0].toString(), range[1].toString());
    }

    private List<DashboardGymObligationItem> loadObligationItems(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(LocalTime.of(23, 59, 59));
        List<Object[]> raw = gymEntranceHistoryRepository.aggregateObligationsByGym(start, end);

        List<Long> gymIds = raw.stream()
                .map(row -> (Long) row[0])
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Gym> gyms = resolveGyms(gymIds);

        List<DashboardGymObligationItem> items = new ArrayList<>(raw.size());
        for (Object[] row : raw) {
            Long gymId = (Long) row[0];
            long count = row[1] instanceof Number n ? n.longValue() : 0L;
            double amount = row[2] instanceof Number n ? n.doubleValue() : 0.0;
            Gym gym = gyms.get(gymId);
            items.add(new DashboardGymObligationItem(
                    gymId,
                    gym != null ? gym.getName() : "Unknown Gym",
                    count,
                    amount
            ));
        }
        return items;
    }

    private Map<Long, Gym> resolveGyms(List<Long> gymIds) {
        Map<Long, Gym> map = new HashMap<>();
        if (gymIds.isEmpty()) {
            return map;
        }
        try {
            for (Gym gym : gymRepository.findWithListDetailsByIdIn(new ArrayList<>(gymIds))) {
                map.put(gym.getId(), gym);
            }
        } catch (Exception e) {
            log.warn("Failed to resolve gyms for dashboard: {}", e.getMessage());
        }
        return map;
    }

    private static String resolveCategory(Gym gym) {
        if (gym == null || gym.getMainCategories() == null || gym.getMainCategories().isEmpty()) {
            return "Zal";
        }
        Category first = gym.getMainCategories().iterator().next();
        return first != null && first.getName() != null ? first.getName() : "Zal";
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

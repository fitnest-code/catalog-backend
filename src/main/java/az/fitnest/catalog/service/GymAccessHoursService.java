package az.fitnest.catalog.service;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.dto.response.GymAccessHoursResponse;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymTimeSlotRule;
import az.fitnest.catalog.model.entity.GymWorkHour;
import az.fitnest.catalog.model.enums.GymWorkHourPeriod;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.GymTimeSlotRuleRepository;
import az.fitnest.catalog.service.homegym.HomeGymRuleService;
import az.fitnest.catalog.service.homegym.HomeGymRuleService.PackageContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Mobile access-hours: gym work hours, package time-slot windows, and {@code forUser}
 * for the caller's active subscription (Asia/Baku).
 *
 * <p>One gym load + one rules query per request. {@code nextOpenAt} scans in memory.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GymAccessHoursService {

    static final ZoneId BAKU = HomeGymRuleService.BAKU;
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59);
    private static final int NEXT_OPEN_HORIZON_DAYS = 14;

    private final GymRepository gymRepository;
    private final GymTimeSlotRuleRepository timeSlotRuleRepository;
    private final HomeGymRuleService homeGymRuleService;
    private final OrderServiceGrpcClient orderServiceGrpcClient;

    public GymAccessHoursResponse getAccessHours(Long gymId, Long userId) {
        return getAccessHours(gymId, userId, LocalDateTime.now(BAKU));
    }

    /** Package-visible for unit tests with a fixed clock. */
    GymAccessHoursResponse getAccessHours(Long gymId, Long userId, LocalDateTime nowBaku) {
        Gym gym = gymRepository.findAccessHoursById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("error.gym_not_found"));

        LocalDate today = nowBaku.toLocalDate();
        LocalTime now = nowBaku.toLocalTime().truncatedTo(ChronoUnit.MINUTES);

        // All rules for the gym once; day filtering stays in memory (also used by nextOpenAt).
        List<GymTimeSlotRule> allRules = timeSlotRuleRepository
                .findByGymIdOrderByEffectiveFromDescIdAsc(gymId);
        boolean hasPackageRestrictions = !allRules.isEmpty();

        GymAccessHoursResponse.GymWorkHoursInfo workingHours = buildWorkingHours(gym);
        List<GymAccessHoursResponse.GymPackageAccess> byPackage = hasPackageRestrictions
                ? buildByPackage(gym, allRules, today)
                : List.of();
        GymAccessHoursResponse.ForUserInfo forUser = buildForUser(
                gym, userId, nowBaku, today, now, allRules, hasPackageRestrictions);

        return new GymAccessHoursResponse(workingHours, byPackage, forUser, hasPackageRestrictions);
    }

    private GymAccessHoursResponse.GymWorkHoursInfo buildWorkingHours(Gym gym) {
        Set<GymWorkHour> hours = gym.getGeneralWorkHours();
        if (hours == null || hours.isEmpty()) {
            return new GymAccessHoursResponse.GymWorkHoursInfo(true, List.of());
        }
        List<GymAccessHoursResponse.GymWorkHourWindow> windows = hours.stream()
                .sorted(Comparator
                        .comparing((GymWorkHour wh) -> wh.getPeriod() == null ? 99 : wh.getPeriod().ordinal())
                        .thenComparing(GymWorkHour::getFromTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(wh -> new GymAccessHoursResponse.GymWorkHourWindow(
                        wh.getPeriod() != null ? wh.getPeriod().name() : null,
                        wh.getFromTime(),
                        wh.getToTime()))
                .toList();
        return new GymAccessHoursResponse.GymWorkHoursInfo(false, windows);
    }

    private List<GymAccessHoursResponse.GymPackageAccess> buildByPackage(
            Gym gym, List<GymTimeSlotRule> rules, LocalDate today) {
        Set<Long> seedIds = new LinkedHashSet<>();
        for (GymTimeSlotRule rule : rules) {
            if (rule.getAllowedPackageIds() != null) {
                seedIds.addAll(rule.getAllowedPackageIds());
            }
            if (rule.getPricePackageId() != null) {
                seedIds.add(rule.getPricePackageId());
            }
        }

        // Warm contexts from seeds, then include every plan that inherits access to any rule.
        Map<Long, PackageContext> contexts = new LinkedHashMap<>();
        for (Long id : seedIds) {
            contexts.put(id, homeGymRuleService.packageContext(id));
        }
        try {
            for (var plan : orderServiceGrpcClient.getGymPlans()) {
                long id = plan.getPackageId();
                contexts.computeIfAbsent(id, homeGymRuleService::packageContext);
            }
        } catch (Exception ignored) {
            // Fall back to seed ids only when the catalog of plans is unavailable.
        }

        List<GymAccessHoursResponse.GymPackageAccess> byPackage = new ArrayList<>();
        for (Map.Entry<Long, PackageContext> entry : contexts.entrySet()) {
            Long pkgId = entry.getKey();
            PackageContext ctx = entry.getValue();
            List<GymAccessHoursResponse.GymAccessWindow> windows = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            boolean unrestrictedAllDay = false;
            Map<DayOfWeek, List<TimeWindow>> byDay = new LinkedHashMap<>();

            for (GymTimeSlotRule rule : rules) {
                if (!isPackageAllowed(rule, ctx)) {
                    continue;
                }
                if (rule.getStartTime() == null && rule.getEndTime() == null) {
                    if (coversEveryDay(rule)) {
                        unrestrictedAllDay = true;
                    }
                    continue;
                }
                if (rule.getStartTime() == null || rule.getEndTime() == null) {
                    continue;
                }
                LocalDate effectiveFrom = rule.getEffectiveFrom() != null ? rule.getEffectiveFrom() : today;
                for (DayOfWeek day : weekdaysOf(rule)) {
                    byDay.computeIfAbsent(day, ignoredDay -> new ArrayList<>())
                            .add(new TimeWindow(rule.getStartTime(), rule.getEndTime()));
                    String key = day.name() + '|' + rule.getStartTime() + '|' + rule.getEndTime()
                            + '|' + effectiveFrom + '|' + rule.getEffectiveTo();
                    if (!seen.add(key)) {
                        continue;
                    }
                    windows.add(new GymAccessHoursResponse.GymAccessWindow(
                            day.name(),
                            effectiveFrom,
                            rule.getEffectiveTo(),
                            rule.getStartTime(),
                            rule.getEndTime(),
                            true));
                }
            }

            boolean fullHours = unrestrictedAllDay || coversGymWorkHours(gym, byDay);
            if (!fullHours && windows.isEmpty()) {
                continue;
            }
            if (fullHours) {
                // Higher tiers that effectively have gym work hours — do not echo lower-tier slots.
                windows = List.of();
            } else {
                windows.sort(Comparator
                        .comparing((GymAccessHoursResponse.GymAccessWindow w) -> dayOrdinal(w.dayName))
                        .thenComparing(w -> w.from, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(w -> w.to, Comparator.nullsLast(Comparator.naturalOrder())));
            }
            byPackage.add(new GymAccessHoursResponse.GymPackageAccess(
                    pkgId, packageDisplayName(ctx, pkgId), fullHours, windows));
        }
        byPackage.sort(Comparator.comparing(p -> p.packageId, Comparator.nullsLast(Comparator.naturalOrder())));
        return byPackage;
    }

    /**
     * True when merged package access covers every general work-hour window
     * (rest days ignored). Used so Silver/Gold/Platinum show {@code fullHours=true}
     * when gap-fill / inheritance yields the full gym schedule.
     */
    private boolean coversGymWorkHours(Gym gym, Map<DayOfWeek, List<TimeWindow>> packageByDay) {
        if (packageByDay.isEmpty()) {
            return false;
        }
        for (DayOfWeek day : DayOfWeek.values()) {
            if (isRestDay(gym, day)) {
                continue;
            }
            List<TimeWindow> work = workWindowsForDay(gym, day);
            if (work.isEmpty()) {
                continue;
            }
            List<TimeWindow> access = mergeWindows(packageByDay.getOrDefault(day, List.of()));
            if (!coversAll(access, work)) {
                return false;
            }
        }
        return true;
    }

    /** Every {@code required} window is fully contained in some merged {@code access} window. */
    private static boolean coversAll(List<TimeWindow> access, List<TimeWindow> required) {
        for (TimeWindow need : required) {
            boolean covered = false;
            for (TimeWindow have : access) {
                if (!have.from().isAfter(need.from()) && !have.to().isBefore(need.to())) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                return false;
            }
        }
        return true;
    }

    private static List<DayOfWeek> weekdaysOf(GymTimeSlotRule rule) {
        String weekdays = rule.getWeekdays();
        if (weekdays == null || weekdays.isBlank()) {
            return List.of(DayOfWeek.values());
        }
        List<DayOfWeek> days = new ArrayList<>();
        for (String part : weekdays.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                days.add(DayOfWeek.valueOf(trimmed.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Skip unknown tokens; same as weekday matching elsewhere.
            }
        }
        return days;
    }

    private static boolean coversEveryDay(GymTimeSlotRule rule) {
        String weekdays = rule.getWeekdays();
        if (weekdays == null || weekdays.isBlank()) {
            return true;
        }
        Set<DayOfWeek> days = new LinkedHashSet<>();
        for (String part : weekdays.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                days.add(DayOfWeek.valueOf(trimmed.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // ignore
            }
        }
        return days.containsAll(List.of(DayOfWeek.values()));
    }

    private static int dayOrdinal(String dayName) {
        if (dayName == null || dayName.isBlank()) {
            return 99;
        }
        try {
            return DayOfWeek.valueOf(dayName).ordinal();
        } catch (IllegalArgumentException ignored) {
            return 99;
        }
    }

    private GymAccessHoursResponse.ForUserInfo buildForUser(
            Gym gym,
            Long userId,
            LocalDateTime nowBaku,
            LocalDate today,
            LocalTime now,
            List<GymTimeSlotRule> allRules,
            boolean hasPackageRestrictions) {
        if (userId == null) {
            return null;
        }

        Long packageId;
        String packageName;
        try {
            var sub = orderServiceGrpcClient.getActiveSubscription(userId);
            if (sub.getSubscriptionStatus() == null
                    || !"ACTIVE".equalsIgnoreCase(sub.getSubscriptionStatus().trim())
                    || sub.getPackageId() <= 0) {
                return null;
            }
            packageId = sub.getPackageId();
            packageName = sub.getPackageName();
        } catch (Exception ignored) {
            return null;
        }

        PackageContext ctx = homeGymRuleService.packageContext(packageId);
        if (packageName == null || packageName.isBlank()) {
            packageName = packageDisplayName(ctx, packageId);
        }

        List<TimeWindow> accessWindows = accessWindowsForDay(
                gym, ctx, allRules, hasPackageRestrictions, today);

        TimeWindow current = findContaining(accessWindows, now);
        if (current != null) {
            LocalDateTime closesAt = LocalDateTime.of(today, current.to());
            int minutesLeft = (int) Math.max(0, ChronoUnit.MINUTES.between(nowBaku, closesAt));
            return new GymAccessHoursResponse.ForUserInfo(
                    packageId, packageName, true, closesAt, null, minutesLeft);
        }

        return new GymAccessHoursResponse.ForUserInfo(
                packageId,
                packageName,
                false,
                null,
                nextOpenAt(gym, ctx, allRules, hasPackageRestrictions, nowBaku),
                0);
    }

    private List<TimeWindow> accessWindowsForDay(
            Gym gym,
            PackageContext ctx,
            List<GymTimeSlotRule> allRules,
            boolean hasPackageRestrictions,
            LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        if (isRestDay(gym, day)) {
            return List.of();
        }

        List<TimeWindow> workWindows = workWindowsForDay(gym, day);
        if (!hasPackageRestrictions) {
            return workWindows.isEmpty() ? List.of(new TimeWindow(LocalTime.MIN, END_OF_DAY)) : workWindows;
        }

        List<TimeWindow> ruleWindows = new ArrayList<>();
        for (GymTimeSlotRule rule : allRules) {
            if (!rule.isEffectiveOn(date) || !matchesWeekday(rule, day) || !isPackageAllowed(rule, ctx)) {
                continue;
            }
            if (rule.getStartTime() == null && rule.getEndTime() == null) {
                return workWindows.isEmpty() ? List.of(new TimeWindow(LocalTime.MIN, END_OF_DAY)) : workWindows;
            }
            if (rule.getStartTime() != null && rule.getEndTime() != null) {
                ruleWindows.add(new TimeWindow(rule.getStartTime(), rule.getEndTime()));
            }
        }
        if (ruleWindows.isEmpty()) {
            return List.of();
        }
        if (workWindows.isEmpty()) {
            return mergeWindows(ruleWindows);
        }
        List<TimeWindow> intersected = new ArrayList<>();
        for (TimeWindow rule : ruleWindows) {
            for (TimeWindow work : workWindows) {
                TimeWindow overlap = intersect(rule, work);
                if (overlap != null) {
                    intersected.add(overlap);
                }
            }
        }
        return mergeWindows(intersected);
    }

    private LocalDateTime nextOpenAt(
            Gym gym,
            PackageContext ctx,
            List<GymTimeSlotRule> allRules,
            boolean hasPackageRestrictions,
            LocalDateTime nowBaku) {
        LocalDate start = nowBaku.toLocalDate();
        LocalTime now = nowBaku.toLocalTime().truncatedTo(ChronoUnit.MINUTES);

        for (int offset = 0; offset < NEXT_OPEN_HORIZON_DAYS; offset++) {
            LocalDate date = start.plusDays(offset);
            List<TimeWindow> access = accessWindowsForDay(
                    gym, ctx, allRules, hasPackageRestrictions, date);
            for (TimeWindow window : access) {
                if (offset == 0) {
                    if (window.from().isAfter(now)) {
                        return LocalDateTime.of(date, window.from());
                    }
                    continue;
                }
                return LocalDateTime.of(date, window.from());
            }
        }
        return null;
    }

    private boolean isRestDay(Gym gym, DayOfWeek day) {
        Set<GymWorkHourPeriod> restDays = gym.getRestDays();
        return restDays != null && restDays.contains(toPeriod(day));
    }

    private List<TimeWindow> workWindowsForDay(Gym gym, DayOfWeek day) {
        Set<GymWorkHour> hours = gym.getGeneralWorkHours();
        if (hours == null || hours.isEmpty()) {
            return List.of();
        }
        GymWorkHourPeriod period = toPeriod(day);
        List<TimeWindow> windows = new ArrayList<>();
        for (GymWorkHour wh : hours) {
            if (wh.getPeriod() == period && wh.getFromTime() != null && wh.getToTime() != null) {
                windows.add(new TimeWindow(wh.getFromTime(), wh.getToTime()));
            }
        }
        return mergeWindows(windows);
    }

    /**
     * Same inheritance as {@link HomeGymRuleService}: a higher-tier subscriber is admitted
     * by a lower-tier rule via {@link PackageContext#eligiblePackageIds()}.
     */
    private static boolean isPackageAllowed(GymTimeSlotRule rule, PackageContext context) {
        Set<Long> allowed = rule.getAllowedPackageIds();
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        if (context == null || context.eligiblePackageIds() == null) {
            return false;
        }
        for (Long allowedPackageId : allowed) {
            if (context.eligiblePackageIds().contains(allowedPackageId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesWeekday(GymTimeSlotRule rule, DayOfWeek day) {
        String weekdays = rule.getWeekdays();
        if (weekdays == null || weekdays.isBlank()) {
            return true;
        }
        for (String part : weekdays.split(",")) {
            if (part.trim().equalsIgnoreCase(day.name())) {
                return true;
            }
        }
        return false;
    }

    private static TimeWindow findContaining(List<TimeWindow> windows, LocalTime minute) {
        for (TimeWindow window : windows) {
            if (!minute.isBefore(window.from()) && !minute.isAfter(window.to())) {
                return window;
            }
        }
        return null;
    }

    private static TimeWindow intersect(TimeWindow a, TimeWindow b) {
        LocalTime from = a.from().isAfter(b.from()) ? a.from() : b.from();
        LocalTime to = a.to().isBefore(b.to()) ? a.to() : b.to();
        return from.isAfter(to) ? null : new TimeWindow(from, to);
    }

    private static List<TimeWindow> mergeWindows(List<TimeWindow> input) {
        if (input.isEmpty()) {
            return List.of();
        }
        List<TimeWindow> sorted = new ArrayList<>(input);
        sorted.sort(Comparator.comparing(TimeWindow::from).thenComparing(TimeWindow::to));
        List<TimeWindow> merged = new ArrayList<>();
        TimeWindow current = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            TimeWindow next = sorted.get(i);
            if (!next.from().isAfter(current.to().plusMinutes(1))) {
                LocalTime end = current.to().isAfter(next.to()) ? current.to() : next.to();
                current = new TimeWindow(current.from(), end);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private static GymWorkHourPeriod toPeriod(DayOfWeek day) {
        return GymWorkHourPeriod.valueOf(day.name());
    }

    private static String packageDisplayName(PackageContext ctx, Long packageId) {
        if (ctx != null && ctx.name() != null && !ctx.name().isBlank()) {
            return ctx.name();
        }
        return "Package " + packageId;
    }

    private record TimeWindow(LocalTime from, LocalTime to) {
    }
}

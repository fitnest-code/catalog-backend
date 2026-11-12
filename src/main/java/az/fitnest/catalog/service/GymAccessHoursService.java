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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Mobile access-hours for a gym: general work hours, per-package windows from
 * home-gym time-slot rules, and a {@code forUser} snapshot for the caller's
 * active subscription (Asia/Baku).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GymAccessHoursService {

    static final ZoneId BAKU = HomeGymRuleService.BAKU;

    private final GymRepository gymRepository;
    private final GymTimeSlotRuleRepository timeSlotRuleRepository;
    private final HomeGymRuleService homeGymRuleService;
    private final OrderServiceGrpcClient orderServiceGrpcClient;

    public GymAccessHoursResponse getAccessHours(Long gymId, Long userId) {
        return getAccessHours(gymId, userId, LocalDateTime.now(BAKU));
    }

    /** Package-visible for unit tests with a fixed clock. */
    GymAccessHoursResponse getAccessHours(Long gymId, Long userId, LocalDateTime nowBaku) {
        Gym gym = gymRepository.findWithDetailsById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("error.gym_not_found"));

        LocalDate today = nowBaku.toLocalDate();
        LocalTime now = nowBaku.toLocalTime().truncatedTo(ChronoUnit.MINUTES);

        GymAccessHoursResponse.GymWorkHoursInfo workingHours = buildWorkingHours(gym);
        List<GymTimeSlotRule> rules = timeSlotRuleRepository.findEffectiveRules(gymId, today);
        List<GymTimeSlotRule> todayRules = rules.stream()
                .filter(rule -> matchesWeekday(rule, today.getDayOfWeek()))
                .toList();

        boolean hasPackageRestrictions = !rules.isEmpty();
        List<GymAccessHoursResponse.GymPackageAccess> byPackage =
                hasPackageRestrictions ? buildByPackage(rules, today) : List.of();

        GymAccessHoursResponse.ForUserInfo forUser = buildForUser(
                gym, userId, nowBaku, today, now, todayRules, hasPackageRestrictions);

        return new GymAccessHoursResponse(workingHours, byPackage, forUser, hasPackageRestrictions);
    }

    private GymAccessHoursResponse.GymWorkHoursInfo buildWorkingHours(Gym gym) {
        Set<GymWorkHour> allWorkHours = gym.getGeneralWorkHours();
        boolean fullHours = allWorkHours == null || allWorkHours.isEmpty();
        List<GymAccessHoursResponse.GymWorkHourWindow> windows = new ArrayList<>();
        if (!fullHours) {
            allWorkHours.stream()
                    .sorted(Comparator
                            .comparing((GymWorkHour wh) -> wh.getPeriod() == null ? 99 : wh.getPeriod().ordinal())
                            .thenComparing(GymWorkHour::getFromTime, Comparator.nullsLast(Comparator.naturalOrder())))
                    .forEach(wh -> windows.add(new GymAccessHoursResponse.GymWorkHourWindow(
                            wh.getPeriod() != null ? wh.getPeriod().name() : null,
                            wh.getFromTime(),
                            wh.getToTime())));
        }
        return new GymAccessHoursResponse.GymWorkHoursInfo(fullHours, windows);
    }

    private List<GymAccessHoursResponse.GymPackageAccess> buildByPackage(
            List<GymTimeSlotRule> rules, LocalDate today) {
        Set<Long> packageIds = new LinkedHashSet<>();
        for (GymTimeSlotRule rule : rules) {
            if (rule.getAllowedPackageIds() != null) {
                packageIds.addAll(rule.getAllowedPackageIds());
            }
            if (rule.getPricePackageId() != null) {
                packageIds.add(rule.getPricePackageId());
            }
        }
        // Include higher tiers that inherit from listed packages so byPackage is complete.
        for (Long listed : new ArrayList<>(packageIds)) {
            PackageContext listedCtx = homeGymRuleService.packageContext(listed);
            int listedRank = listedCtx.rank();
            if (listedRank <= 0) {
                continue;
            }
            for (long candidate = 1; candidate <= 4; candidate++) {
                PackageContext ctx = homeGymRuleService.packageContext(candidate);
                if (ctx.rank() >= listedRank) {
                    packageIds.add(candidate);
                }
            }
        }

        List<GymAccessHoursResponse.GymPackageAccess> byPackage = new ArrayList<>();
        for (Long pkgId : packageIds) {
            PackageContext ctx = homeGymRuleService.packageContext(pkgId);
            String name = packageDisplayName(ctx, pkgId);
            List<GymAccessHoursResponse.GymAccessWindow> windows = new ArrayList<>();
            boolean sawExplicitWindow = false;
            for (GymTimeSlotRule rule : rules) {
                if (!isPackageAllowed(rule, ctx)) {
                    continue;
                }
                if (rule.getStartTime() == null && rule.getEndTime() == null) {
                    // All-day rule for this package — treat as fullHours for that package.
                    continue;
                }
                sawExplicitWindow = true;
                windows.add(new GymAccessHoursResponse.GymAccessWindow(
                        rule.getEffectiveFrom() != null ? rule.getEffectiveFrom() : today,
                        rule.getEffectiveTo(),
                        rule.getStartTime(),
                        rule.getEndTime(),
                        true));
            }
            boolean fullHours = !sawExplicitWindow;
            windows.sort(Comparator
                    .comparing((GymAccessHoursResponse.GymAccessWindow w) -> w.from,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(w -> w.to, Comparator.nullsLast(Comparator.naturalOrder())));
            byPackage.add(new GymAccessHoursResponse.GymPackageAccess(
                    pkgId, name, fullHours, windows));
        }
        byPackage.sort(Comparator.comparing(p -> p.packageId, Comparator.nullsLast(Comparator.naturalOrder())));
        return byPackage;
    }

    private GymAccessHoursResponse.ForUserInfo buildForUser(
            Gym gym,
            Long userId,
            LocalDateTime nowBaku,
            LocalDate today,
            LocalTime now,
            List<GymTimeSlotRule> todayRules,
            boolean hasPackageRestrictions) {
        if (userId == null) {
            return null;
        }

        Long packageId = null;
        String packageName = null;
        try {
            var sub = orderServiceGrpcClient.getActiveSubscription(userId);
            String status = sub.getSubscriptionStatus();
            if (status != null
                    && "ACTIVE".equalsIgnoreCase(status.trim())
                    && sub.getPackageId() > 0) {
                packageId = sub.getPackageId();
                packageName = sub.getPackageName();
            }
        } catch (Exception ignored) {
            return null;
        }
        if (packageId == null) {
            return null;
        }

        PackageContext ctx = homeGymRuleService.packageContext(packageId);
        if (packageName == null || packageName.isBlank()) {
            packageName = packageDisplayName(ctx, packageId);
        }

        if (isRestDay(gym, today.getDayOfWeek())) {
            return new GymAccessHoursResponse.ForUserInfo(
                    packageId,
                    packageName,
                    false,
                    null,
                    nextOpenAt(gym, ctx, todayRules, hasPackageRestrictions, nowBaku),
                    0);
        }

        List<TimeWindow> workWindows = workWindowsForDay(gym, today.getDayOfWeek());
        boolean outsideWorkHours = !workWindows.isEmpty()
                && workWindows.stream().noneMatch(w -> contains(w, now));
        if (outsideWorkHours) {
            return new GymAccessHoursResponse.ForUserInfo(
                    packageId,
                    packageName,
                    false,
                    null,
                    nextOpenAt(gym, ctx, todayRules, hasPackageRestrictions, nowBaku),
                    0);
        }

        List<TimeWindow> accessWindows = resolveAccessWindows(
                gym, ctx, todayRules, hasPackageRestrictions, today.getDayOfWeek(), workWindows);

        Optional<TimeWindow> current = accessWindows.stream()
                .filter(w -> contains(w, now))
                .findFirst();
        if (current.isPresent()) {
            LocalDateTime closesAt = LocalDateTime.of(today, current.get().to());
            int minutesLeft = (int) Math.max(0, ChronoUnit.MINUTES.between(nowBaku, closesAt));
            return new GymAccessHoursResponse.ForUserInfo(
                    packageId, packageName, true, closesAt, null, minutesLeft);
        }

        return new GymAccessHoursResponse.ForUserInfo(
                packageId,
                packageName,
                false,
                null,
                nextOpenAt(gym, ctx, todayRules, hasPackageRestrictions, nowBaku),
                0);
    }

    private List<TimeWindow> resolveAccessWindows(
            Gym gym,
            PackageContext ctx,
            List<GymTimeSlotRule> todayRules,
            boolean hasPackageRestrictions,
            DayOfWeek day,
            List<TimeWindow> workWindows) {
        if (!hasPackageRestrictions) {
            return workWindows.isEmpty()
                    ? List.of(new TimeWindow(LocalTime.MIN, LocalTime.of(23, 59)))
                    : workWindows;
        }

        List<TimeWindow> ruleWindows = new ArrayList<>();
        for (GymTimeSlotRule rule : todayRules) {
            if (!isPackageAllowed(rule, ctx)) {
                continue;
            }
            if (rule.getStartTime() == null && rule.getEndTime() == null) {
                return workWindows.isEmpty()
                        ? List.of(new TimeWindow(LocalTime.MIN, LocalTime.of(23, 59)))
                        : workWindows;
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
                intersect(rule, work).ifPresent(intersected::add);
            }
        }
        return mergeWindows(intersected);
    }

    private LocalDateTime nextOpenAt(
            Gym gym,
            PackageContext ctx,
            List<GymTimeSlotRule> ignoredTodayRules,
            boolean hasPackageRestrictions,
            LocalDateTime nowBaku) {
        LocalDate start = nowBaku.toLocalDate();
        LocalTime now = nowBaku.toLocalTime().truncatedTo(ChronoUnit.MINUTES);

        for (int offset = 0; offset < 14; offset++) {
            LocalDate date = start.plusDays(offset);
            DayOfWeek day = date.getDayOfWeek();
            if (isRestDay(gym, day)) {
                continue;
            }
            List<GymTimeSlotRule> dayRules = hasPackageRestrictions
                    ? timeSlotRuleRepository.findEffectiveRules(gym.getId(), date).stream()
                    .filter(rule -> matchesWeekday(rule, day))
                    .toList()
                    : List.of();
            List<TimeWindow> workWindows = workWindowsForDay(gym, day);
            List<TimeWindow> access = resolveAccessWindows(
                    gym, ctx, dayRules, hasPackageRestrictions, day, workWindows);
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
        if (restDays == null || restDays.isEmpty()) {
            return false;
        }
        GymWorkHourPeriod period = toPeriod(day);
        return period != null && restDays.contains(period);
    }

    private List<TimeWindow> workWindowsForDay(Gym gym, DayOfWeek day) {
        Set<GymWorkHour> hours = gym.getGeneralWorkHours();
        if (hours == null || hours.isEmpty()) {
            return List.of();
        }
        GymWorkHourPeriod period = toPeriod(day);
        List<TimeWindow> windows = new ArrayList<>();
        for (GymWorkHour wh : hours) {
            if (wh.getPeriod() != period || wh.getFromTime() == null || wh.getToTime() == null) {
                continue;
            }
            windows.add(new TimeWindow(wh.getFromTime(), wh.getToTime()));
        }
        return mergeWindows(windows);
    }

    private boolean isPackageAllowed(GymTimeSlotRule rule, PackageContext context) {
        Set<Long> allowed = rule.getAllowedPackageIds();
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        if (context == null) {
            return false;
        }
        int rank = context.rank();
        for (Long allowedPackageId : allowed) {
            if (context.eligiblePackageIds() != null
                    && context.eligiblePackageIds().contains(allowedPackageId)) {
                return true;
            }
            if (allowedPackageId != null) {
                int allowedRank = homeGymRuleService.packageContext(allowedPackageId).rank();
                if (allowedRank > 0 && rank > 0 && allowedRank <= rank) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesWeekday(GymTimeSlotRule rule, DayOfWeek day) {
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

    private static boolean contains(TimeWindow window, LocalTime minute) {
        return !minute.isBefore(window.from()) && !minute.isAfter(window.to());
    }

    private static Optional<TimeWindow> intersect(TimeWindow a, TimeWindow b) {
        LocalTime from = a.from().isAfter(b.from()) ? a.from() : b.from();
        LocalTime to = a.to().isBefore(b.to()) ? a.to() : b.to();
        if (from.isAfter(to)) {
            return Optional.empty();
        }
        return Optional.of(new TimeWindow(from, to));
    }

    private static List<TimeWindow> mergeWindows(List<TimeWindow> input) {
        if (input.isEmpty()) {
            return List.of();
        }
        List<TimeWindow> sorted = input.stream()
                .sorted(Comparator.comparing(TimeWindow::from).thenComparing(TimeWindow::to))
                .toList();
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
        return switch (day) {
            case MONDAY -> GymWorkHourPeriod.MONDAY;
            case TUESDAY -> GymWorkHourPeriod.TUESDAY;
            case WEDNESDAY -> GymWorkHourPeriod.WEDNESDAY;
            case THURSDAY -> GymWorkHourPeriod.THURSDAY;
            case FRIDAY -> GymWorkHourPeriod.FRIDAY;
            case SATURDAY -> GymWorkHourPeriod.SATURDAY;
            case SUNDAY -> GymWorkHourPeriod.SUNDAY;
        };
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

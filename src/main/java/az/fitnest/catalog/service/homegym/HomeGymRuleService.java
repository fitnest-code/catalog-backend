package az.fitnest.catalog.service.homegym;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymPackagePrice;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.model.entity.GymTimeSlotRule;
import az.fitnest.catalog.repository.GymPackagePriceRepository;
import az.fitnest.catalog.repository.GymTimeSlotRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves home-gym package hierarchy, time-slot rules and price snapshots for
 * catalog (spec R1/R14/R15/R17/R18). Order-backend remains the source of truth
 * for visit decisions; catalog only prepares the snapshots passed over gRPC.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeGymRuleService {

    public static final ZoneId BAKU = ZoneId.of("Asia/Baku");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> ALL_DAYS = Set.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
    private static final long PACKAGE_CONTEXT_TTL_MILLIS = 10 * 60 * 1000L;
    private static final ConcurrentHashMap<Long, CachedPackageContext> PACKAGE_CONTEXT_CACHE =
            new ConcurrentHashMap<>();

    private final GymPackagePriceRepository packagePriceRepository;
    private final GymTimeSlotRuleRepository timeSlotRuleRepository;
    private final OrderServiceGrpcClient orderServiceGrpcClient;

    public record PackageContext(Long packageId, String name, int rank, List<Long> eligiblePackageIds) {
    }

    public record PriceSnapshot(String pricePackageName, Double singlePrice, Double unlimitedPrice) {
    }

    private record CachedPackageContext(PackageContext context, long expiresAt) {
    }

    public record CheckInResolution(boolean allowed, String reason, Long ruleId, String pricePackageName,
                                    Double singlePrice, Double unlimitedPrice, String availableHours) {
        public static CheckInResolution rejected(String reason, String availableHours) {
            return new CheckInResolution(false, reason, null, null, null, null, availableHours);
        }
    }

    public PackageContext packageContext(Long packageId) {
        if (packageId == null) {
            return new PackageContext(null, null, 0, List.of());
        }
        CachedPackageContext cached = PACKAGE_CONTEXT_CACHE.get(packageId);
        if (cached != null && cached.expiresAt() > System.currentTimeMillis()) {
            return cached.context();
        }
        PackageContext resolved = loadPackageContext(packageId);
        if (resolved == null) {
            // Never cache a transient gRPC failure: it would degrade rank/eligibility
            // resolution for this package until the next deployment.
            return new PackageContext(packageId, null, 0, List.of(packageId));
        }
        PACKAGE_CONTEXT_CACHE.put(packageId,
                new CachedPackageContext(resolved, System.currentTimeMillis() + PACKAGE_CONTEXT_TTL_MILLIS));
        return resolved;
    }

    private PackageContext loadPackageContext(Long packageId) {
        List<az.fitnest.order.grpc.SubscriptionPackageInfo> plans;
        try {
            plans = orderServiceGrpcClient.getGymPlans();
        } catch (Exception e) {
            return null;
        }
        String name = null;
        for (var plan : plans) {
            if (plan.getPackageId() == packageId.longValue()) {
                name = plan.getName();
                break;
            }
        }
        if (name == null) {
            try {
                var infos = orderServiceGrpcClient.getPackageNamesByIds(List.of(packageId));
                if (!infos.isEmpty()) {
                    name = infos.get(0).getName();
                }
            } catch (Exception ignored) {
                // fall through with unknown rank
            }
        }
        int rank = packageRank(name);
        List<Long> eligible = new ArrayList<>();
        for (var plan : plans) {
            if (packageRank(plan.getName()) <= rank) {
                eligible.add(plan.getPackageId());
            }
        }
        if (!eligible.contains(packageId)) {
            eligible.add(packageId);
        }
        return new PackageContext(packageId, name, rank, List.copyOf(eligible));
    }

    public List<Long> eligiblePackageIds(Long packageId) {
        List<Long> ids = packageContext(packageId).eligiblePackageIds();
        return ids.isEmpty() ? List.of(-1L) : ids;
    }

    public boolean supportsPackage(Gym gym, PackageContext context) {
        if (gym == null || gym.getSubscriptions() == null || context == null
                || context.eligiblePackageIds().isEmpty()) {
            return false;
        }
        for (GymSubscription subscription : gym.getSubscriptions()) {
            if (subscription.getPackageId() != null
                    && context.eligiblePackageIds().contains(subscription.getPackageId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * R18 master switch plus the optional package-level override on the latest price row.
     */
    public boolean isHomeEligible(Gym gym, Long packageId) {
        if (gym == null || Boolean.FALSE.equals(gym.getHomeEligible())) {
            return false;
        }
        GymPackagePrice price = latestPrice(gym.getId(), packageId);
        return price == null || !Boolean.FALSE.equals(price.getHomeEligible());
    }

    /**
     * R14/R15: resolve the matching rule and the price snapshot before the order call.
     */
    public CheckInResolution resolveForCheckIn(Gym gym, PackageContext context, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        List<GymTimeSlotRule> rules = timeSlotRuleRepository.findEffectiveRules(gym.getId(), today);
        String availableHours = computeAvailableHours(rules, context);
        if (rules.isEmpty()) {
            PriceSnapshot price = priceFor(gym, context.packageId(), today);
            return new CheckInResolution(true, null, null, price.pricePackageName(),
                    price.singlePrice(), price.unlimitedPrice(), null);
        }

        List<GymTimeSlotRule> matching = rules.stream()
                .filter(rule -> matchesWeekday(rule, now.getDayOfWeek()))
                .filter(rule -> matchesTime(rule, now.toLocalTime()))
                .toList();
        if (matching.isEmpty()) {
            return CheckInResolution.rejected("OUTSIDE_HOURS", availableHours);
        }

        List<GymTimeSlotRule> allowed = matching.stream()
                .filter(rule -> isPackageAllowed(rule, context))
                .sorted(Comparator
                        .comparing((GymTimeSlotRule rule) -> !hasExplicitPackages(rule))
                        .thenComparing(GymTimeSlotRule::getEffectiveFrom,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(GymTimeSlotRule::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        if (allowed.isEmpty()) {
            return CheckInResolution.rejected("TIER", availableHours);
        }

        GymTimeSlotRule rule = allowed.get(0);
        Long pricePackageId = rule.getPricePackageId() != null
                ? rule.getPricePackageId() : context.packageId();
        PriceSnapshot price = priceFor(gym, pricePackageId, today);
        return new CheckInResolution(true, null, rule.getId(), price.pricePackageName(),
                price.singlePrice(), price.unlimitedPrice(), availableHours);
    }

    /**
     * Hours during which the subscriber's package can enter the gym. {@code null}
     * means no restriction (no rules, or a rule allowing the package all day).
     */
    public String availableHours(Gym gym, PackageContext context) {
        List<GymTimeSlotRule> rules = timeSlotRuleRepository.findEffectiveRules(
                gym.getId(), LocalDate.now(BAKU));
        return computeAvailableHours(rules, context);
    }

    private String computeAvailableHours(List<GymTimeSlotRule> rules, PackageContext context) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        boolean unrestricted = rules.stream()
                .filter(rule -> isPackageAllowed(rule, context))
                .anyMatch(rule -> isAllDay(rule) && coversEveryDay(rule));
        if (unrestricted) {
            return null;
        }
        List<String> windows = new ArrayList<>();
        for (GymTimeSlotRule rule : rules) {
            if (!isPackageAllowed(rule, context) || isAllDay(rule)) {
                continue;
            }
            String window = formatWindow(rule);
            if (window != null && !windows.contains(window)) {
                windows.add(window);
            }
        }
        if (windows.isEmpty()) {
            return null;
        }
        return String.join(", ", windows);
    }

    private PriceSnapshot priceFor(Gym gym, Long pricePackageId, LocalDate date) {
        String packageName = packageContext(pricePackageId).name();
        GymPackagePrice row = latestPrice(gym.getId(), pricePackageId);
        Double single = null;
        Double unlimited = null;
        if (row != null) {
            single = row.getSinglePrice();
            unlimited = row.getUnlimitedPrice();
        }
        if (single == null || unlimited == null) {
            Double dailyPrice = null;
            if (gym.getSubscriptions() != null) {
                for (GymSubscription subscription : gym.getSubscriptions()) {
                    if (subscription.getPackageId() != null
                            && subscription.getPackageId().equals(pricePackageId)) {
                        dailyPrice = subscription.getDailyPrice();
                        break;
                    }
                }
            }
            if (single == null) {
                single = dailyPrice != null ? dailyPrice : 0.0;
            }
            if (unlimited == null) {
                unlimited = dailyPrice != null ? dailyPrice : single;
            }
        }
        return new PriceSnapshot(packageName, single, unlimited);
    }

    private GymPackagePrice latestPrice(Long gymId, Long packageId) {
        if (gymId == null || packageId == null) {
            return null;
        }
        return packagePriceRepository
                .findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        gymId, packageId, LocalDate.now(BAKU))
                .orElse(null);
    }

    private boolean isPackageAllowed(GymTimeSlotRule rule, PackageContext context) {
        if (!hasExplicitPackages(rule)) {
            return true;
        }
        if (context == null) {
            return false;
        }
        int rank = context.rank();
        for (Long allowedPackageId : rule.getAllowedPackageIds()) {
            if (context.eligiblePackageIds().contains(allowedPackageId)) {
                return true;
            }
            if (allowedPackageId != null) {
                int allowedRank = packageRank(packageContext(allowedPackageId).name());
                // Fail closed when either package name is unknown (rank 0).
                if (allowedRank > 0 && rank > 0 && allowedRank <= rank) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasExplicitPackages(GymTimeSlotRule rule) {
        return rule.getAllowedPackageIds() != null && !rule.getAllowedPackageIds().isEmpty();
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

    private boolean matchesTime(GymTimeSlotRule rule, LocalTime now) {
        LocalTime start = rule.getStartTime();
        LocalTime end = rule.getEndTime();
        if (start == null && end == null) {
            return true;
        }
        if (start == null) {
            return !now.isAfter(end);
        }
        if (end == null) {
            return !now.isBefore(start);
        }
        if (!start.isAfter(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        }
        return !now.isBefore(start) || !now.isAfter(end);
    }

    private boolean isAllDay(GymTimeSlotRule rule) {
        return rule.getStartTime() == null && rule.getEndTime() == null;
    }

    private boolean coversEveryDay(GymTimeSlotRule rule) {
        String weekdays = rule.getWeekdays();
        if (weekdays == null || weekdays.isBlank()) {
            return true;
        }
        Set<String> days = new java.util.HashSet<>();
        for (String part : weekdays.split(",")) {
            days.add(part.trim().toUpperCase(Locale.ROOT));
        }
        return days.containsAll(ALL_DAYS);
    }

    private String formatWindow(GymTimeSlotRule rule) {
        if (rule.getStartTime() == null || rule.getEndTime() == null) {
            return null;
        }
        return rule.getStartTime().format(TIME_FORMAT) + "-" + rule.getEndTime().format(TIME_FORMAT);
    }

    private int packageRank(String packageName) {
        if (packageName == null) {
            return 0;
        }
        String lower = packageName.toLowerCase(Locale.ROOT);
        if (lower.contains("platinum")) {
            return 4;
        }
        if (lower.contains("gold")) {
            return 3;
        }
        if (lower.contains("silver")) {
            return 2;
        }
        if (lower.contains("bronze")) {
            return 1;
        }
        return 0;
    }
}

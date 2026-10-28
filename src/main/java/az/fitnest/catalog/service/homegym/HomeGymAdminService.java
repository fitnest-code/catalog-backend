package az.fitnest.catalog.service.homegym;

import az.fitnest.catalog.dto.request.HomeGymPackagePriceRequest;
import az.fitnest.catalog.dto.request.HomeGymTimeSlotRequest;
import az.fitnest.catalog.dto.request.HomeGymToggleRequest;
import az.fitnest.catalog.dto.response.HomeGymAdminStateResponse;
import az.fitnest.catalog.dto.response.HomeGymPackagePriceResponse;
import az.fitnest.catalog.dto.response.HomeGymTimeSlotRuleResponse;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ForbiddenException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.exception.UnprocessableEntityException;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymPackagePrice;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.model.entity.GymTimeSlotRule;
import az.fitnest.catalog.model.entity.GymWorkHour;
import az.fitnest.catalog.repository.GymAdminRepository;
import az.fitnest.catalog.repository.GymPackagePriceRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.GymTimeSlotRuleRepository;
import az.fitnest.catalog.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Admin management for the home-gym feature (spec mock 6.1-6.3, R14/R15/R17/R18).
 *
 * <p>Prices are versioned by the first day of a month, so an ordinary change
 * writes a new row for next month instead of mutating the current one. Time-slot
 * rules follow the same rule (F-03): a change to a currently effective rule closes
 * the old row at the end of the current month and opens a new one next month.
 * The master switch publishes an R18 event consumed by order-backend.
 */
@Service
@RequiredArgsConstructor
public class HomeGymAdminService {

    public static final String HOME_GYM_EVENTS_TOPIC = "home-gym-events";
    public static final String HOME_GYM_DISABLED = "HOME_GYM_DISABLED";

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("AZ", "EN", "RU");
    private static final Set<String> ALL_WEEKDAYS = Set.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");

    private final GymRepository gymRepository;
    private final GymAdminRepository gymAdminRepository;
    private final GymPackagePriceRepository packagePriceRepository;
    private final GymTimeSlotRuleRepository timeSlotRuleRepository;
    private final HomeGymRuleService ruleService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ---------------------------------------------------------------------
    // Master switch (R18)
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public HomeGymAdminStateResponse getState(Long gymId) {
        return state(requireGym(gymId));
    }

    @Transactional
    public HomeGymAdminStateResponse setHomeEligible(Long gymId, HomeGymToggleRequest request) {
        if (request == null || request.getEnabled() == null) {
            throw new BadRequestException("HOME_GYM_INVALID_REQUEST", "error.home_gym.invalid_request");
        }
        Gym gym = requireGym(gymId);
        boolean wasEligible = !Boolean.FALSE.equals(gym.getHomeEligible());
        boolean enabled = request.getEnabled();
        if (wasEligible == enabled) {
            return state(gym);
        }

        gym.setHomeEligible(enabled);
        gymRepository.save(gym);

        if (!enabled) {
            Map<String, String> title = requireLocalized(request.getTitle(), "title");
            Map<String, String> body = requireLocalized(request.getBody(), "body");
            List<Long> recipients = distinctIds(request.getRecipients());

            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventType", HOME_GYM_DISABLED);
            event.put("gymId", gym.getId());
            event.put("title", title);
            event.put("body", body);
            event.put("recipients", recipients);
            event.put("createdBy", UserContext.getCurrentUserId());
            kafkaTemplate.send(HOME_GYM_EVENTS_TOPIC, String.valueOf(gym.getId()), event);
        }
        return state(gym);
    }

    // ---------------------------------------------------------------------
    // Package prices (R15/R17)
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<HomeGymPackagePriceResponse> getPrices(Long gymId) {
        Gym gym = requireGym(gymId);
        return prices(gym);
    }

    @Transactional
    public HomeGymPackagePriceResponse upsertPrice(Long gymId, HomeGymPackagePriceRequest request) {
        if (request == null || request.getPackageId() == null) {
            throw new BadRequestException("HOME_GYM_INVALID_REQUEST", "error.home_gym.invalid_request");
        }
        Gym gym = requireGym(gymId);
        if (!offeredPackageIds(gym).contains(request.getPackageId())) {
            throw new BadRequestException("HOME_GYM_PACKAGE_NOT_IN_GYM", "error.home_gym.package_not_in_gym");
        }
        requireNonNegative(request.getSinglePrice(), "singlePrice");
        requireNonNegative(request.getUnlimitedPrice(), "unlimitedPrice");
        boolean homeEligible = Boolean.TRUE.equals(request.getHomeEligible());
        if (homeEligible && request.getUnlimitedPrice() == null) {
            throw new BadRequestException("HOME_GYM_INVALID_PRICE", "error.home_gym.unlimited_price_required");
        }

        LocalDate effectiveFrom = effectiveMonth(request.getEffectiveFrom());
        GymPackagePrice price = packagePriceRepository
                .findByGymIdAndPackageIdAndEffectiveFrom(gymId, request.getPackageId(), effectiveFrom)
                .orElseGet(GymPackagePrice::new);
        price.setGym(gym);
        price.setPackageId(request.getPackageId());
        price.setIncluded(request.getIncluded() == null ? Boolean.TRUE : request.getIncluded());
        price.setHomeEligible(request.getHomeEligible());
        price.setSinglePrice(request.getSinglePrice());
        price.setUnlimitedPrice(request.getUnlimitedPrice());
        price.setEffectiveFrom(effectiveFrom);
        return toPriceResponse(packagePriceRepository.save(price));
    }

    // ---------------------------------------------------------------------
    // Time-slot rules (R14/R15, admin validations F-03)
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<HomeGymTimeSlotRuleResponse> getTimeSlots(Long gymId) {
        Gym gym = requireGym(gymId);
        return timeSlots(gym);
    }

    @Transactional
    public HomeGymTimeSlotRuleResponse createTimeSlot(Long gymId, HomeGymTimeSlotRequest request) {
        if (request == null) {
            throw new BadRequestException("HOME_GYM_INVALID_REQUEST", "error.home_gym.invalid_request");
        }
        Gym gym = requireGym(gymId);
        LocalDate effectiveFrom = resolveEffectiveFrom(request.getEffectiveFrom());
        LocalDate effectiveTo = resolveEffectiveTo(request.getEffectiveTo(), effectiveFrom);
        TimeSlotValues values = validateTimeSlot(gym, request, effectiveFrom, effectiveTo);
        validateNoOverlap(gymId, null, values, effectiveFrom, effectiveTo);

        GymTimeSlotRule rule = new GymTimeSlotRule();
        rule.setGym(gym);
        applyTimeSlot(rule, values, effectiveFrom, effectiveTo);
        return toTimeSlotResponse(timeSlotRuleRepository.save(rule));
    }

    @Transactional
    public HomeGymTimeSlotRuleResponse updateTimeSlot(Long gymId, Long ruleId, HomeGymTimeSlotRequest request) {
        if (request == null) {
            throw new BadRequestException("HOME_GYM_INVALID_REQUEST", "error.home_gym.invalid_request");
        }
        Gym gym = requireGym(gymId);
        GymTimeSlotRule rule = timeSlotRuleRepository.findByIdAndGymId(ruleId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "HOME_GYM_TIME_SLOT_NOT_FOUND", "error.home_gym.time_slot_not_found"));

        LocalDate today = LocalDate.now(HomeGymRuleService.BAKU);
        LocalDate nextMonthStart = nextMonthStart();
        LocalDate requestedFrom = resolveEffectiveFrom(request.getEffectiveFrom());
        // A future rule that has not taken effect yet is edited in place. Everything else is
        // versioned (F-03): the old row is closed before the new one becomes effective.
        boolean futureRule = rule.getEffectiveFrom() != null && rule.getEffectiveFrom().isAfter(today);
        LocalDate effectiveFrom = futureRule ? rule.getEffectiveFrom() : requestedFrom;
        LocalDate effectiveTo = resolveEffectiveTo(request.getEffectiveTo(), effectiveFrom);
        TimeSlotValues values = validateTimeSlot(gym, request, effectiveFrom, effectiveTo);
        validateNoOverlap(gymId, ruleId, values, effectiveFrom, effectiveTo);

        if (rule.getEffectiveFrom() != null && !rule.getEffectiveFrom().isAfter(today)) {
            // Currently effective: keep history until the day before the new version.
            LocalDate closeAt = effectiveFrom.minusDays(1);
            if (closeAt.isBefore(rule.getEffectiveFrom())) {
                closeAt = rule.getEffectiveFrom();
            }
            rule.setEffectiveTo(closeAt);
            timeSlotRuleRepository.save(rule);

            GymTimeSlotRule version = new GymTimeSlotRule();
            version.setGym(gym);
            applyTimeSlot(version, values, effectiveFrom, effectiveTo);
            return toTimeSlotResponse(timeSlotRuleRepository.save(version));
        }

        if (futureRule && !rule.getEffectiveFrom().isBefore(nextMonthStart)) {
            // Not effective yet: edit in place.
            applyTimeSlot(rule, values, effectiveFrom, effectiveTo);
            return toTimeSlotResponse(timeSlotRuleRepository.save(rule));
        }

        // Historical row: leave it untouched and add the new version.
        GymTimeSlotRule version = new GymTimeSlotRule();
        version.setGym(gym);
        applyTimeSlot(version, values, effectiveFrom, effectiveTo);
        return toTimeSlotResponse(timeSlotRuleRepository.save(version));
    }

    @Transactional
    public void deleteTimeSlot(Long gymId, Long ruleId) {
        requireGym(gymId);
        GymTimeSlotRule rule = timeSlotRuleRepository.findByIdAndGymId(ruleId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "HOME_GYM_TIME_SLOT_NOT_FOUND", "error.home_gym.time_slot_not_found"));

        LocalDate today = LocalDate.now(HomeGymRuleService.BAKU);
        boolean currentlyEffective = rule.getEffectiveFrom() != null
                && !rule.getEffectiveFrom().isAfter(today)
                && (rule.getEffectiveTo() == null || !rule.getEffectiveTo().isBefore(today));
        if (currentlyEffective) {
            // F-03: the current rule keeps working until the end of the month.
            LocalDate closeAt = nextMonthStart().minusDays(1);
            if (!closeAt.isBefore(rule.getEffectiveFrom())) {
                rule.setEffectiveTo(closeAt);
                timeSlotRuleRepository.save(rule);
                return;
            }
        }
        timeSlotRuleRepository.delete(rule);
    }

    // ---------------------------------------------------------------------
    // Validation helpers
    // ---------------------------------------------------------------------

    private record TimeSlotValues(Set<String> weekdays, LocalTime start, LocalTime end,
                                  Set<Long> allowedPackageIds, Long pricePackageId) {
    }

    private TimeSlotValues validateTimeSlot(Gym gym, HomeGymTimeSlotRequest request,
                                            LocalDate effectiveFrom, LocalDate effectiveTo) {
        LocalTime start = parseTime(request.getStartTime(), "startTime");
        LocalTime end = parseTime(request.getEndTime(), "endTime");
        if (start == null || end == null || !start.isBefore(end)) {
            // v3: start < end, inside the same day (no overnight windows).
            throw new BadRequestException("HOME_GYM_INVALID_TIME_WINDOW", "error.home_gym.invalid_time_window");
        }
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new BadRequestException("HOME_GYM_INVALID_EFFECTIVE_RANGE", "error.home_gym.invalid_effective_range");
        }

        Set<String> weekdays = normalizeWeekdays(request.getWeekdays());
        if (weekdays.isEmpty()) {
            throw new BadRequestException("HOME_GYM_INVALID_WEEKDAY", "error.home_gym.invalid_weekday");
        }
        validateWithinWorkHours(gym, weekdays, start, end);

        Set<Long> offered = offeredPackageIds(gym);
        Set<Long> allowed = new LinkedHashSet<>(distinctIds(request.getAllowedPackageIds()));
        if (allowed.isEmpty()) {
            throw new BadRequestException("HOME_GYM_ALLOWED_PACKAGES_REQUIRED",
                    "error.home_gym.allowed_packages_required");
        }
        int minOfferedRank = minimumOfferedRank(offered);
        for (Long packageId : allowed) {
            validateGymPackage(packageId, offered, minOfferedRank);
        }

        Long pricePackageId = request.getPricePackageId();
        if (pricePackageId == null) {
            throw new BadRequestException("HOME_GYM_PRICE_PACKAGE_REQUIRED",
                    "error.home_gym.price_package_required");
        }
        validateGymPackage(pricePackageId, offered, minOfferedRank);
        GymPackagePrice price = packagePriceRepository
                .findByGymIdAndPackageIdAndEffectiveFrom(gym.getId(), pricePackageId, effectiveFrom)
                .orElse(null);
        if (price != null && Boolean.FALSE.equals(price.getIncluded())) {
            throw new UnprocessableEntityException("HOME_GYM_PRICE_PACKAGE_NOT_INCLUDED",
                    "error.home_gym.price_package_not_included");
        }

        return new TimeSlotValues(weekdays, start, end, allowed, pricePackageId);
    }

    private void validateGymPackage(Long packageId, Set<Long> offered, int minOfferedRank) {
        if (offered.contains(packageId)) {
            return;
        }
        int rank = ruleService.packageContext(packageId).rank();
        // Packages above an offered one inherit access to the gym; unknown ranks fail closed.
        if (rank > 0 && minOfferedRank > 0 && rank >= minOfferedRank) {
            return;
        }
        throw new BadRequestException("HOME_GYM_PACKAGE_NOT_IN_GYM", "error.home_gym.package_not_in_gym");
    }

    private int minimumOfferedRank(Set<Long> offered) {
        int min = 0;
        for (Long packageId : offered) {
            int rank = ruleService.packageContext(packageId).rank();
            if (rank > 0 && (min == 0 || rank < min)) {
                min = rank;
            }
        }
        return min;
    }

    private void validateWithinWorkHours(Gym gym, Set<String> weekdays, LocalTime start, LocalTime end) {
        Set<GymWorkHour> workHours = gym.getGeneralWorkHours();
        if (workHours == null || workHours.isEmpty()) {
            return;
        }
        for (String weekday : weekdays) {
            GymWorkHour window = workHours.stream()
                    .filter(hour -> hour != null && hour.getPeriod() != null
                            && weekday.equals(hour.getPeriod().name()))
                    .findFirst()
                    .orElse(null);
            if (window == null) {
                throw new BadRequestException("HOME_GYM_OUTSIDE_WORK_HOURS",
                        "error.home_gym.outside_work_hours");
            }
            if (window.getFromTime() != null && start.isBefore(window.getFromTime())) {
                throw new BadRequestException("HOME_GYM_OUTSIDE_WORK_HOURS",
                        "error.home_gym.outside_work_hours");
            }
            if (window.getToTime() != null && end.isAfter(window.getToTime())) {
                throw new BadRequestException("HOME_GYM_OUTSIDE_WORK_HOURS",
                        "error.home_gym.outside_work_hours");
            }
        }
    }

    private void validateNoOverlap(Long gymId, Long excludeRuleId, TimeSlotValues values,
                                   LocalDate effectiveFrom, LocalDate effectiveTo) {
        for (GymTimeSlotRule other : timeSlotRuleRepository.findByGymIdOrderByEffectiveFromDescIdAsc(gymId)) {
            if (other.getId() != null && other.getId().equals(excludeRuleId)) {
                continue;
            }
            if (!other.isEffectiveOn(effectiveFrom)) {
                continue;
            }
            if (effectiveTo != null && other.getEffectiveFrom() != null
                    && other.getEffectiveFrom().isAfter(effectiveTo)) {
                continue;
            }
            if (!daysIntersect(values.weekdays(), other.getWeekdays())) {
                continue;
            }
            LocalTime otherStart = other.getStartTime();
            LocalTime otherEnd = other.getEndTime();
            if (otherStart == null || otherEnd == null) {
                // Legacy all-day rules overlap every window.
                throw overlap();
            }
            // Windows are inclusive on the minute (16:59 ends before 17:00 starts).
            if (!values.start().isAfter(otherEnd) && !otherStart.isAfter(values.end())) {
                throw overlap();
            }
        }
    }

    private UnprocessableEntityException overlap() {
        return new UnprocessableEntityException("TIME_SLOT_OVERLAP", "error.home_gym.time_slot_overlap");
    }

    private boolean daysIntersect(Set<String> weekdays, String otherWeekdays) {
        if (otherWeekdays == null || otherWeekdays.isBlank()) {
            return true;
        }
        for (String part : otherWeekdays.split(",")) {
            if (weekdays.contains(part.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private LocalDate resolveEffectiveFrom(LocalDate requested) {
        LocalDate nextMonthStart = nextMonthStart();
        if (requested == null) {
            return nextMonthStart;
        }
        LocalDate date = requested.withDayOfMonth(1);
        return date.isBefore(nextMonthStart) ? nextMonthStart : date;
    }

    private LocalDate resolveEffectiveTo(LocalDate requested, LocalDate effectiveFrom) {
        if (requested == null) {
            return null;
        }
        return requested.isBefore(effectiveFrom) ? effectiveFrom : requested;
    }

    private LocalDate nextMonthStart() {
        return LocalDate.now(HomeGymRuleService.BAKU).withDayOfMonth(1).plusMonths(1);
    }

    private void applyTimeSlot(GymTimeSlotRule rule, TimeSlotValues values,
                               LocalDate effectiveFrom, LocalDate effectiveTo) {
        rule.setWeekdays(values.weekdays().isEmpty() ? null : String.join(",", values.weekdays()));
        rule.setStartTime(values.start());
        rule.setEndTime(values.end());
        rule.setAllowedPackageIds(new LinkedHashSet<>(values.allowedPackageIds()));
        rule.setPricePackageId(values.pricePackageId());
        rule.setEffectiveFrom(effectiveFrom);
        rule.setEffectiveTo(effectiveTo);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private HomeGymAdminStateResponse state(Gym gym) {
        return HomeGymAdminStateResponse.builder()
                .gymId(gym.getId())
                .gymName(gym.getName())
                .homeEligible(!Boolean.FALSE.equals(gym.getHomeEligible()))
                .prices(prices(gym))
                .timeSlots(timeSlots(gym))
                .build();
    }

    private List<HomeGymPackagePriceResponse> prices(Gym gym) {
        return packagePriceRepository.findByGymIdOrderByEffectiveFromDescPackageIdAsc(gym.getId())
                .stream()
                .map(this::toPriceResponse)
                .toList();
    }

    private List<HomeGymTimeSlotRuleResponse> timeSlots(Gym gym) {
        return timeSlotRuleRepository.findByGymIdOrderByEffectiveFromDescIdAsc(gym.getId())
                .stream()
                .map(this::toTimeSlotResponse)
                .toList();
    }

    private HomeGymPackagePriceResponse toPriceResponse(GymPackagePrice price) {
        return HomeGymPackagePriceResponse.builder()
                .id(price.getId())
                .packageId(price.getPackageId())
                .packageName(ruleService.packageContext(price.getPackageId()).name())
                .included(price.getIncluded())
                .homeEligible(price.getHomeEligible())
                .singlePrice(price.getSinglePrice())
                .unlimitedPrice(price.getUnlimitedPrice())
                .effectiveFrom(price.getEffectiveFrom())
                .build();
    }

    private HomeGymTimeSlotRuleResponse toTimeSlotResponse(GymTimeSlotRule rule) {
        List<Long> allowed = rule.getAllowedPackageIds() == null
                ? List.of() : new ArrayList<>(rule.getAllowedPackageIds());
        return HomeGymTimeSlotRuleResponse.builder()
                .id(rule.getId())
                .weekdays(splitWeekdays(rule.getWeekdays()))
                .startTime(rule.getStartTime() != null ? rule.getStartTime().toString() : null)
                .endTime(rule.getEndTime() != null ? rule.getEndTime().toString() : null)
                .allowedPackageIds(allowed)
                .pricePackageId(rule.getPricePackageId())
                .pricePackageName(rule.getPricePackageId() != null
                        ? ruleService.packageContext(rule.getPricePackageId()).name() : null)
                .effectiveFrom(rule.getEffectiveFrom())
                .effectiveTo(rule.getEffectiveTo())
                .build();
    }

    private Gym requireGym(Long gymId) {
        if (gymId == null) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found");
        }
        Gym gym = gymRepository.findWithDetailsById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "error.gym_not_found"));
        ensureGymAccess(gymId);
        return gym;
    }

    private void ensureGymAccess(Long gymId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (isAdmin) {
            return;
        }
        Long userId = UserContext.getCurrentUserId();
        if (userId == null || !gymAdminRepository.existsByGymIdAndUserId(gymId, userId)) {
            throw new ForbiddenException("error.forbidden", "FORBIDDEN");
        }
    }

    private Set<Long> offeredPackageIds(Gym gym) {
        Set<Long> ids = new LinkedHashSet<>();
        if (gym.getSubscriptions() != null) {
            for (GymSubscription subscription : gym.getSubscriptions()) {
                if (subscription.getPackageId() != null) {
                    ids.add(subscription.getPackageId());
                }
            }
        }
        return ids;
    }

    private LocalDate effectiveMonth(LocalDate requested) {
        LocalDate nextMonthStart = nextMonthStart();
        if (requested == null) {
            return nextMonthStart;
        }
        LocalDate date = requested.withDayOfMonth(1);
        return date.isBefore(nextMonthStart) ? nextMonthStart : date;
    }

    private void requireNonNegative(Double value, String field) {
        if (value == null || value < 0) {
            throw new BadRequestException("HOME_GYM_INVALID_PRICE", "error.home_gym.invalid_price");
        }
    }

    private LocalTime parseTime(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("HOME_GYM_INVALID_TIME_WINDOW", "error.home_gym.invalid_time_window");
        }
    }

    private Set<String> normalizeWeekdays(List<String> weekdays) {
        Set<String> normalized = new LinkedHashSet<>();
        if (weekdays == null || weekdays.isEmpty()) {
            return normalized;
        }
        for (String weekday : weekdays) {
            if (weekday == null || weekday.isBlank()) {
                continue;
            }
            String value = weekday.trim().toUpperCase(Locale.ROOT);
            if (!ALL_WEEKDAYS.contains(value)) {
                throw new BadRequestException("HOME_GYM_INVALID_WEEKDAY", "error.home_gym.invalid_weekday");
            }
            normalized.add(value);
        }
        return normalized;
    }

    private List<String> splitWeekdays(String weekdays) {
        if (weekdays == null || weekdays.isBlank()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String part : weekdays.split(",")) {
            if (!part.isBlank()) {
                result.add(part.trim().toUpperCase(Locale.ROOT));
            }
        }
        return result;
    }

    private Map<String, String> requireLocalized(Map<String, String> input, String field) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (input != null) {
            input.forEach((key, value) -> {
                if (key != null && value != null && !value.isBlank()) {
                    normalized.put(key.trim().toUpperCase(Locale.ROOT), value.trim());
                }
            });
        }
        for (String language : SUPPORTED_LANGUAGES) {
            if (!normalized.containsKey(language)) {
                throw new BadRequestException("HOME_GYM_MISSING_TRANSLATION",
                        "error.home_gym.missing_translation");
            }
        }
        return normalized;
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(ids.stream()
                .filter(java.util.Objects::nonNull)
                .toList()));
    }
}

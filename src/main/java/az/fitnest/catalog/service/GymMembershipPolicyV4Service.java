package az.fitnest.catalog.service;

import az.fitnest.catalog.client.OrderServiceGrpcClient;
import az.fitnest.catalog.dto.v4.GymMembershipPolicyV4;
import az.fitnest.catalog.dto.v4.GymMembershipPolicyV4.*;
import az.fitnest.catalog.model.entity.*;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GymMembershipPolicyV4Service {
  private final GymMembershipPolicyV4Repository policies;
  private final GymRepository gyms;
  private final GymPackagePriceRepository packagePrices;
  private final GymPrimaryDisableV4Repository disables;
  private final PrimaryGymNotificationV4Service notifications;
  private final OrderServiceGrpcClient plans;
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  public static final ZoneId ZONE = ZoneId.of("Asia/Baku");

  @Transactional(readOnly = true)
  public GymMembershipPolicyV4 get(Long gymId) {
    return decode(
        policies
            .findById(gymId)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "POLICY_NOT_CONFIGURED")));
  }

  /**
   * Non-throwing lookup for callers already inside a read-only transaction (e.g. gym detail).
   * {@link #get} marks the outer TX rollback-only when the policy is missing because
   * {@code ResponseStatusException} is a runtime exception — swallowing it still fails commit.
   */
  @Transactional(readOnly = true)
  public Optional<GymMembershipPolicyV4> find(Long gymId) {
    return policies.findById(gymId).map(this::decode);
  }

  private GymMembershipPolicyV4 decode(GymMembershipPolicyV4Entity row) {
    try {
      var p = mapper.readValue(row.getPayload(), GymMembershipPolicyV4.class);
      return new GymMembershipPolicyV4(
          row.getVersion(), p.timeRulesEnabled(), p.packages(), p.timeRules());
    } catch (Exception e) {
      throw new IllegalStateException("Invalid stored gym policy", e);
    }
  }

  /**
   * Package ids the gym currently sells per V4 policy ({@code included} or {@code primaryEnabled}).
   * Gyms without a policy are omitted — callers treat omission as "use all gym_subscriptions rows".
   * Batched so list endpoints do one DB round-trip instead of N.
   */
  @Transactional(readOnly = true)
  public Map<Long, Set<Long>> offeredPackageIdsByGym(Collection<Long> gymIds) {
    if (gymIds == null || gymIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, Set<Long>> out = new HashMap<>();
    for (GymMembershipPolicyV4Entity row : policies.findAllById(gymIds)) {
      if (row == null || row.getGymId() == null) {
        continue;
      }
      out.put(row.getGymId(), offeredPackageIds(decode(row)));
    }
    return out;
  }

  /** Package ids marked included or primary-enabled on the given policy (empty when none). */
  public static Set<Long> offeredPackageIds(GymMembershipPolicyV4 policy) {
    if (policy == null || policy.packages() == null || policy.packages().isEmpty()) {
      return Set.of();
    }
    Set<Long> ids = new LinkedHashSet<>();
    for (PackageRule rule : policy.packages()) {
      if (rule != null
          && rule.packageId() != null
          && (rule.included() || rule.primaryEnabled())) {
        ids.add(rule.packageId());
      }
    }
    return ids;
  }

  @Transactional
  // Policy drives public tier badges / supportedSubscriptions — drop Redis list+detail caches so
  // landing and app stop serving stale Bronze min-entry after admin toggles included packages.
  @CacheEvict(
      cacheNames = {
        "home-eligible-gym-count",
        "main-page-gyms",
        "gym-listings",
        "gym-count-by-subscription",
        "landing-gyms",
        "landing-home-gyms",
        "landing-gym-detail",
        "landing-gym-filters",
        "landing-gyms-v2",
        "landing-gym-detail-v2",
        "landing-gym-filters-v2",
        "landing-stats"
      },
      allEntries = true)
  public GymMembershipPolicyV4 save(
      Long gymId, az.fitnest.catalog.dto.v4.GymPolicyUpdateV4 update) {
    var input = update.policy();
    Gym gym = gyms.findById(gymId).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "GYM_NOT_FOUND"));
    validate(input, gym);
    var row = policies.findById(gymId).orElseGet(GymMembershipPolicyV4Entity::new);
    if (row.getGymId() != null && row.getVersion() != input.version())
      throw error(HttpStatus.CONFLICT, "POLICY_CHANGED");
    var old = row.getGymId() == null ? null : decode(row);
    if (old == null && input.version() != 0) throw error(HttpStatus.CONFLICT, "POLICY_CHANGED");
    if (old != null
        && old.packages().stream()
            .anyMatch(
                a ->
                    input.packages().stream()
                        .noneMatch(
                            b -> a.packageId().equals(b.packageId()) && a.tier() == b.tier())))
      throw error(HttpStatus.BAD_REQUEST, "PACKAGE_TIER_MAPPING_IMMUTABLE");
    var now = LocalDateTime.now(ZONE);
    var normalized =
        input.packages().stream()
            .map(
                p -> {
                  var prior =
                      old == null
                          ? null
                          : old.packages().stream()
                              .filter(x -> x.packageId().equals(p.packageId()))
                              .findFirst()
                              .orElse(null);
                  LocalDateTime disabled =
                      p.primaryEnabled()
                          ? prior == null ? null : prior.primaryDisabledAt()
                          : prior != null
                                  && !prior.primaryEnabled()
                                  && prior.primaryDisabledAt() != null
                              ? prior.primaryDisabledAt()
                              : now;
                  return new PackageRule(
                      p.packageId(),
                      p.tier(),
                      p.included(),
                      p.primaryEnabled(),
                      p.entryPrice(),
                      p.primaryMonthlyPrice() != null
                          ? p.primaryMonthlyPrice()
                          : prior == null ? null : prior.primaryMonthlyPrice(),
                      p.categories() == null ? List.of() : List.copyOf(p.categories()),
                      disabled);
                })
            .toList();
    row.setGymId(gymId);
    try {
      row.setPayload(
          mapper.writeValueAsString(
              new GymMembershipPolicyV4(
                  input.version(),
                  input.timeRulesEnabled(),
                  normalized,
                  input.timeRules() == null ? List.of() : input.timeRules())));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    var disabled = new HashSet<Long>();
    if (old != null)
      for (var member : old.packages()) {
        var prior =
            member.included()
                ? member
                : old.packages().stream()
                    .filter(x -> x.included() && x.tier().ordinal() <= member.tier().ordinal())
                    .min(Comparator.comparing(PackageRule::tier))
                    .orElse(null);
        var next =
            normalized.stream()
                .filter(x -> x.packageId().equals(member.packageId()))
                .findFirst()
                .orElse(null);
        if (next != null && !next.included())
          next =
              normalized.stream()
                  .filter(x -> x.included() && x.tier().ordinal() <= member.tier().ordinal())
                  .min(Comparator.comparing(PackageRule::tier))
                  .orElse(null);
        if (prior != null && prior.primaryEnabled() && (next == null || !next.primaryEnabled()))
          disabled.add(member.packageId());
      }
    for (Long packageId : disabled) {
      var event = new GymPrimaryDisableV4();
      event.setGymId(gymId);
      event.setPackageId(packageId);
      event.setDisabledAt(now);
      disables.save(event);
    }
    notifications.record(gymId, disabled, update);
    policies.saveAndFlush(row);
    // Mobile list APIs read gyms.home_eligible / gym_package_price — keep them aligned with
    // admin "Əsas zal kimi seçilə bilər" (primaryEnabled), which defaults to false.
    syncHomeEligibleFlags(gym, normalized, now.toLocalDate());
    ensurePrimarySubscriptions(gym, normalized);
    return decode(row);
  }

  /**
   * QR entrance ({@code HomeGymRuleService.supportsPackage}) and v4 detail
   * {@code supportedSubscriptions} both read {@code gym_subscriptions}. V4 policy previously only
   * inserted a row when {@code primaryEnabled} was true, so {@code included=true} packages were
   * written to {@code gym_package_price} but missing from {@code gym_subscriptions} — members on
   * that tier got {@code GYM_NOT_SUPPORTED}. Sync every included package (not only primary). Never
   * deletes or edits existing rows, so admin-configured prices/services are untouched.
   */
  private void ensurePrimarySubscriptions(Gym gym, List<PackageRule> packages) {
    boolean added = false;
    for (PackageRule rule : packages) {
      if (!rule.included() || rule.packageId() == null) continue;
      boolean present =
          gym.getSubscriptions().stream().anyMatch(s -> rule.packageId().equals(s.getPackageId()));
      if (present) continue;
      GymSubscription subscription = new GymSubscription();
      subscription.setGym(gym);
      subscription.setPackageId(rule.packageId());
      if (rule.entryPrice() != null) {
        subscription.setDailyPrice(rule.entryPrice().doubleValue());
      }
      subscription.setSupportedServices(new HashSet<>());
      gym.getSubscriptions().add(subscription);
      added = true;
    }
    if (added) gyms.save(gym);
  }

  /**
   * Bridges v4 membership policy into the v1 home-gym columns used by selectable-gym lists.
   * Master switch is on only when at least one tier has {@code primaryEnabled=true}.
   */
  private void syncHomeEligibleFlags(Gym gym, List<PackageRule> packages, LocalDate today) {
    boolean anyPrimary = packages.stream().anyMatch(PackageRule::primaryEnabled);
    gym.setHomeEligible(anyPrimary);
    gyms.save(gym);

    LocalDate effectiveFrom = today.withDayOfMonth(1);
    for (PackageRule rule : packages) {
      GymPackagePrice price =
          packagePrices
              .findByGymIdAndPackageIdAndEffectiveFrom(
                  gym.getId(), rule.packageId(), effectiveFrom)
              .orElseGet(GymPackagePrice::new);
      price.setGym(gym);
      price.setPackageId(rule.packageId());
      price.setIncluded(rule.included());
      price.setHomeEligible(rule.primaryEnabled());
      if (rule.primaryMonthlyPrice() != null) {
        price.setUnlimitedPrice(rule.primaryMonthlyPrice().doubleValue());
      }
      if (rule.entryPrice() != null) {
        price.setSinglePrice(rule.entryPrice().doubleValue());
      }
      price.setEffectiveFrom(effectiveFrom);
      packagePrices.save(price);
    }
  }

  /**
   * The admin UI shows "Bitmə 00:00" for "until end of day", but a raw 00:00 end would be a
   * midnight *start* and leave a gap after the last real window (everyone denied).
   */
  static LocalTime effectiveEnd(LocalTime start, LocalTime end) {
    if (end.equals(LocalTime.MIDNIGHT) && start.isAfter(LocalTime.MIDNIGHT))
      return LocalTime.of(23, 59);
    return end;
  }

  /**
   * Tier windows inherit upward (as legacy home-gym): a rule admits a member when any allowed tier
   * is at or below the member's tier, so Bronze-allowed windows also admit Silver/Gold/Platinum.
   */
  static boolean tierAdmitted(Set<Tier> allowed, Tier member) {
    return allowed != null
        && member != null
        && allowed.stream().anyMatch(t -> t.ordinal() <= member.ordinal());
  }

  public static void validateRules(GymMembershipPolicyV4 p) {
    if (p.packages() == null
        || p.packages().size() != 4
        || p.packages().stream().map(PackageRule::tier).distinct().count() != 4
        || p.packages().stream().map(PackageRule::packageId).distinct().count() != 4)
      throw error(HttpStatus.BAD_REQUEST, "FOUR_UNIQUE_PACKAGES_REQUIRED");
    for (var r : p.packages()) {
      if (r.primaryEnabled() && (!r.included() || r.primaryMonthlyPrice() == null))
        throw error(HttpStatus.BAD_REQUEST, "PRIMARY_PRICE_REQUIRED");
    }
    if (!p.timeRulesEnabled()) return;
    if (p.timeRules() == null || p.timeRules().isEmpty())
      throw error(HttpStatus.BAD_REQUEST, "TIME_RULES_REQUIRED");
    for (var r : p.timeRules()) {
      if (r.start().getSecond() != 0
          || r.start().getNano() != 0
          || r.end().getSecond() != 0
          || r.end().getNano() != 0
          || r.start().isAfter(effectiveEnd(r.start(), r.end()))) throw error(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE");
      var price =
          p.packages().stream().filter(x -> x.tier() == r.priceTier() && x.included()).findFirst();
      if (price.isPresent()
          && price.get().primaryMonthlyPrice() == null
          && p.packages().stream()
              .anyMatch(x -> x.primaryEnabled() && tierAdmitted(r.allowedTiers(), x.tier())))
        throw error(HttpStatus.BAD_REQUEST, "TIME_PRIMARY_PRICE_REQUIRED");
      if (price.isEmpty()
          || r.allowedTiers().stream().anyMatch(t -> t.ordinal() < r.priceTier().ordinal()))
        throw error(HttpStatus.BAD_REQUEST, "INVALID_TIME_PRICE_TIER");
    }
    for (var day : DayOfWeek.values()) {
      var ranges =
          p.timeRules().stream()
              .filter(r -> r.days().contains(day))
              .sorted(Comparator.comparing(TimeRule::start))
              .toList();
      for (int i = 1; i < ranges.size(); i++) {
        var prev = ranges.get(i - 1);
        if (!ranges.get(i).start().isAfter(effectiveEnd(prev.start(), prev.end())))
          throw error(HttpStatus.BAD_REQUEST, "OVERLAPPING_TIME_RULES");
      }
    }
  }

  private void validate(GymMembershipPolicyV4 p, Gym gym) {
    validateRules(p);
    for (var r : p.packages())
      if (!plans.checkPackageExists(r.packageId()))
        throw error(HttpStatus.BAD_REQUEST, "PACKAGE_NOT_ACTIVE");
    if (!p.timeRulesEnabled()) return;
    // Per-package schedules may leave work-hour gaps (nobody enters). Rules may not extend
    // outside configured gym work hours — those minutes are always GYM_CLOSED at quote time.
    for (var r : p.timeRules()) {
      int startMin = r.start().getHour() * 60 + r.start().getMinute();
      LocalTime end = effectiveEnd(r.start(), r.end());
      int endMin = end.getHour() * 60 + end.getMinute();
      for (var day : r.days()) {
        for (int minute = startMin; minute <= endMin; minute++) {
          if (!isWorking(gym, day, LocalTime.of(minute / 60, minute % 60)))
            throw error(HttpStatus.BAD_REQUEST, "TIME_RULES_OUTSIDE_WORK_HOURS");
        }
      }
    }
  }

  @Transactional(readOnly = true)
  public Quote quote(Long gymId, Long packageId, LocalDateTime at) {
    var gym = gyms.findById(gymId).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "GYM_NOT_FOUND"));
    var p = get(gymId);
    var member =
        p.packages().stream()
            .filter(r -> r.packageId().equals(packageId))
            .findFirst()
            .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "UNKNOWN_PACKAGE"));
    var base =
        member.included()
            ? member
            : p.packages().stream()
                .filter(r -> r.included() && r.tier().ordinal() <= member.tier().ordinal())
                .min(Comparator.comparing(PackageRule::tier))
                .orElse(null);
    String reason = null;
    if (gym.getStatus() != GymStatus.ACTIVE) reason = "GYM_INACTIVE";
    boolean working = isWorking(gym, at.getDayOfWeek(), at.toLocalTime());
    if (!working) reason = "GYM_CLOSED";
    if (base == null) reason = "PACKAGE_NOT_INCLUDED";
    if (p.timeRulesEnabled()) {
      var range =
          p.timeRules().stream()
              .filter(
                  r ->
                      r.days().contains(at.getDayOfWeek())
                          && !at.toLocalTime().withSecond(0).withNano(0).isBefore(r.start())
                          && !at.toLocalTime()
                              .withSecond(0)
                              .withNano(0)
                              .isAfter(effectiveEnd(r.start(), r.end())))
              .findFirst()
              .orElse(null);
      if (range == null || !tierAdmitted(range.allowedTiers(), member.tier()))
        reason = "TIME_ACCESS_DENIED";
      else
        base =
            p.packages().stream()
                .filter(r -> r.tier() == range.priceTier())
                .findFirst()
                .orElseThrow();
    }
    // Primary eligibility follows the member's included tier, or the inherited lower tier.
    var primary =
        member.included()
            ? member
            : p.packages().stream()
                .filter(r -> r.included() && r.tier().ordinal() <= member.tier().ordinal())
                .min(Comparator.comparing(PackageRule::tier))
                .orElse(null);
    return new Quote(
        gymId,
        gym.getName(),
        p.version(),
        reason == null,
        gym.getStatus() == GymStatus.ACTIVE && primary != null && primary.primaryEnabled(),
        primary == null ? null : primary.primaryDisabledAt(),
        base == null ? null : base.entryPrice(),
        base == null ? null : base.primaryMonthlyPrice(),
        base == null ? null : base.tier(),
        reason,
        disables.findByGymIdAndPackageIdOrderByDisabledAtAsc(gymId, packageId).stream()
            .map(GymPrimaryDisableV4::getDisabledAt)
            .toList(),
        accessWindows(p, member, gym));
  }

  private List<AccessWindow> accessWindows(
      GymMembershipPolicyV4 policy, PackageRule member, Gym gym) {
    if (policy.timeRulesEnabled())
      return policy.timeRules().stream()
          .filter(r -> tierAdmitted(r.allowedTiers(), member.tier()))
          .map(
              r -> {
                var price =
                    policy.packages().stream()
                        .filter(p -> p.tier() == r.priceTier())
                        .findFirst()
                        .orElseThrow();
                return new AccessWindow(
                    r.days(),
                    r.start(),
                    effectiveEnd(r.start(), r.end()),
                    r.priceTier(),
                    price.entryPrice(),
                    price.primaryMonthlyPrice());
              })
          .toList();
    var base =
        member.included()
            ? member
            : policy.packages().stream()
                .filter(p -> p.included() && p.tier().ordinal() <= member.tier().ordinal())
                .min(Comparator.comparing(PackageRule::tier))
                .orElse(null);
    if (base == null) return List.of();
    return gym.getGeneralWorkHours().stream()
        .filter(h -> !gym.getRestDays().contains(h.getPeriod()))
        .map(
            h ->
                new AccessWindow(
                    Set.of(DayOfWeek.valueOf(h.getPeriod().name())),
                    h.getFromTime(),
                    h.getToTime(),
                    base.tier(),
                    base.entryPrice(),
                    base.primaryMonthlyPrice()))
        .toList();
  }

  public static boolean isWorking(Gym gym, DayOfWeek day, LocalTime rawTime) {
    LocalTime time = rawTime.withSecond(0).withNano(0);
    if (gym.getRestDays().stream().anyMatch(d -> d.name().equals(day.name()))) return false;
    return gym.getGeneralWorkHours().stream()
        .anyMatch(
            h -> {
              if (h.getFromTime() == null || h.getToTime() == null || h.getPeriod() == null)
                return false;
              boolean same = h.getPeriod().name().equals(day.name());
              if (!h.getFromTime().isAfter(h.getToTime()))
                return same && !time.isBefore(h.getFromTime()) && !time.isAfter(h.getToTime());
              return same && !time.isBefore(h.getFromTime())
                  || h.getPeriod().name().equals(day.minus(1).name())
                      && !time.isAfter(h.getToTime());
            });
  }

  private static ResponseStatusException error(HttpStatus status, String code) {
    return new ResponseStatusException(status, code);
  }
}

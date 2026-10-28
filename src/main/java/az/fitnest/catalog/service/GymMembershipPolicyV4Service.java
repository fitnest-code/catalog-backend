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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GymMembershipPolicyV4Service {
  private final GymMembershipPolicyV4Repository policies;
  private final GymRepository gyms;
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

  private GymMembershipPolicyV4 decode(GymMembershipPolicyV4Entity row) {
    try {
      var p = mapper.readValue(row.getPayload(), GymMembershipPolicyV4.class);
      return new GymMembershipPolicyV4(
          row.getVersion(), p.timeRulesEnabled(), p.packages(), p.timeRules());
    } catch (Exception e) {
      throw new IllegalStateException("Invalid stored gym policy", e);
    }
  }

  @Transactional
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
    return decode(row);
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
          || r.start().isAfter(r.end())) throw error(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE");
      var price =
          p.packages().stream().filter(x -> x.tier() == r.priceTier() && x.included()).findFirst();
      if (price.isPresent()
          && price.get().primaryMonthlyPrice() == null
          && p.packages().stream()
              .anyMatch(x -> x.primaryEnabled() && r.allowedTiers().contains(x.tier())))
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
      for (int i = 1; i < ranges.size(); i++)
        if (!ranges.get(i).start().isAfter(ranges.get(i - 1).end()))
          throw error(HttpStatus.BAD_REQUEST, "OVERLAPPING_TIME_RULES");
    }
  }

  private void validate(GymMembershipPolicyV4 p, Gym gym) {
    validateRules(p);
    for (var r : p.packages())
      if (!plans.checkPackageExists(r.packageId()))
        throw error(HttpStatus.BAD_REQUEST, "PACKAGE_NOT_ACTIVE");
    if (!p.timeRulesEnabled()) return;
    // Inclusive minute ranges must cover exactly the configured working minutes, with no gaps.
    for (var day : DayOfWeek.values()) {
      for (int minute = 0; minute < 1440; minute++) {
        final LocalTime time = LocalTime.of(minute / 60, minute % 60);
        boolean working = isWorking(gym, day, time);
        boolean covered =
            p.timeRules().stream()
                .anyMatch(
                    r ->
                        r.days().contains(day)
                            && !time.isBefore(r.start())
                            && !time.isAfter(r.end()));
        if (working != covered)
          throw error(HttpStatus.BAD_REQUEST, "TIME_RULES_MUST_COVER_WORK_HOURS");
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
                          && !at.toLocalTime().withSecond(0).withNano(0).isAfter(r.end()))
              .findFirst()
              .orElse(null);
      if (range == null || !range.allowedTiers().contains(member.tier()))
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
          .filter(r -> r.allowedTiers().contains(member.tier()))
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
                    r.end(),
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

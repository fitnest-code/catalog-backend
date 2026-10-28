package az.fitnest.catalog.dto.v4;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public record GymMembershipPolicyV4(
    long version,
    boolean timeRulesEnabled,
    @NotEmpty List<@NotNull @Valid PackageRule> packages,
    List<@NotNull @Valid TimeRule> timeRules) {
  public record PackageRule(
      @NotNull @Positive Long packageId,
      @NotNull Tier tier,
      boolean included,
      boolean primaryEnabled,
      @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal entryPrice,
      @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal primaryMonthlyPrice,
      List<@NotBlank String> categories,
      LocalDateTime primaryDisabledAt) {}

  public record TimeRule(
      @NotEmpty Set<@NotNull DayOfWeek> days,
      @NotNull LocalTime start,
      @NotNull LocalTime end,
      @NotEmpty Set<@NotNull Tier> allowedTiers,
      @NotNull Tier priceTier) {}

  public enum Tier {
    BRONZE,
    SILVER,
    GOLD,
    PLATINUM
  }

  public record Quote(
      Long gymId,
      String gymName,
      long policyVersion,
      boolean allowed,
      boolean primaryEnabled,
      LocalDateTime primaryDisabledAt,
      BigDecimal entryPrice,
      BigDecimal primaryMonthlyPrice,
      Tier priceTier,
      String reason,
      List<LocalDateTime> primaryDisableHistory,
      List<AccessWindow> accessWindows) {}

  public record AccessWindow(
      Set<DayOfWeek> days,
      LocalTime start,
      LocalTime end,
      Tier priceTier,
      BigDecimal entryPrice,
      BigDecimal primaryMonthlyPrice) {}
}

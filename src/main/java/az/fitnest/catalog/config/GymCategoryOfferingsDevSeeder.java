package az.fitnest.catalog.config;

import az.fitnest.catalog.dto.request.CreateTranslationRequest;
import az.fitnest.catalog.dto.v4.GymMembershipPolicyV4;
import az.fitnest.catalog.dto.v4.GymMembershipPolicyV4.PackageRule;
import az.fitnest.catalog.dto.v4.GymMembershipPolicyV4.Tier;
import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymCategoryPackagePrice;
import az.fitnest.catalog.model.entity.GymCategoryServiceLink;
import az.fitnest.catalog.model.entity.GymCategoryWorkHour;
import az.fitnest.catalog.model.entity.GymDescription;
import az.fitnest.catalog.model.entity.GymEntranceHistory;
import az.fitnest.catalog.model.entity.GymMembershipPolicyV4Entity;
import az.fitnest.catalog.model.entity.GymPackagePrice;
import az.fitnest.catalog.model.entity.GymSubscription;
import az.fitnest.catalog.model.entity.GymWorkHour;
import az.fitnest.catalog.model.entity.SupportedService;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.model.enums.GymWorkHourPeriod;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.GymCategoryPackagePriceRepository;
import az.fitnest.catalog.repository.GymCategoryServiceLinkRepository;
import az.fitnest.catalog.repository.GymCategoryWorkHourRepository;
import az.fitnest.catalog.repository.GymEntranceHistoryRepository;
import az.fitnest.catalog.repository.GymMembershipPolicyV4Repository;
import az.fitnest.catalog.repository.GymPackagePriceRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.SupportedServiceRepository;
import az.fitnest.catalog.service.TranslationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Profiles;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * DEV-only mock gyms for category / primary-check-in QA. Prefer one fully filled gym over many thin
 * stubs; keep a couple of edge scenarios for picker/price failures.
 *
 * <p>Activation: profile {@code dev|development|local}, or force with {@code
 * fitnest.dev.seed-category-scenarios=true}. Idempotent by gym name — delete {@code [DEV-SCENARIO] *}
 * to re-seed.
 */
@Slf4j
@Component
@Conditional(GymCategoryOfferingsDevSeeder.DevSeedCondition.class)
public class GymCategoryOfferingsDevSeeder implements ApplicationRunner {

  public static final String PROPERTY = "fitnest.dev.seed-category-scenarios";
  public static final String PREFIX = "[DEV-SCENARIO] ";
  public static final String FULLY_FILLED = PREFIX + "Fully Filled";
  public static final String MAIN_ONLY = PREFIX + "Main-only Gym";
  public static final String MULTI_SUB = PREFIX + "Multi-Sub Mixed";

  /** Default Bronze→Platinum package ids used across FitNest DEV/tests. */
  static final long[] PACKAGE_IDS = {1201L, 1202L, 1203L, 1204L};

  static final Tier[] PACKAGE_TIERS = {Tier.BRONZE, Tier.SILVER, Tier.GOLD, Tier.PLATINUM};
  static final long DEV_USER_ID = 900_000_001L;
  private static final ZoneId BAKU = ZoneId.of("Asia/Baku");
  private static final LocalDate PRICE_FROM = LocalDate.of(2026, 1, 1);

  private final CategoryRepository categories;
  private final GymRepository gyms;
  private final GymPackagePriceRepository gymPrices;
  private final GymCategoryPackagePriceRepository categoryPrices;
  private final GymCategoryWorkHourRepository categoryWorkHours;
  private final GymCategoryServiceLinkRepository categoryServices;
  private final SupportedServiceRepository supportedServices;
  private final GymEntranceHistoryRepository history;
  private final GymMembershipPolicyV4Repository policies;
  private final TranslationService translations;
  private final TransactionTemplate tx;
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  private static final Map<String, String[]> NAME_I18N =
      Map.ofEntries(
          Map.entry("fitness", new String[] {"Fitness", "Фитнес"}),
          Map.entry("yoga", new String[] {"Yoga", "Йога"}),
          Map.entry("pilates", new String[] {"Pilates", "Пилатес"}),
          Map.entry("hamam", new String[] {"Bath", "Баня"}),
          Map.entry("sauna", new String[] {"Sauna", "Сауна"}),
          Map.entry("pool", new String[] {"Pool", "Бассейн"}),
          Map.entry("crossfit", new String[] {"CrossFit", "Кроссфит"}),
          Map.entry("boxing", new String[] {"Boxing", "Бокс"}),
          Map.entry("boks", new String[] {"Boxing", "Бокс"}));

  public GymCategoryOfferingsDevSeeder(
      CategoryRepository categories,
      GymRepository gyms,
      GymPackagePriceRepository gymPrices,
      GymCategoryPackagePriceRepository categoryPrices,
      GymCategoryWorkHourRepository categoryWorkHours,
      GymCategoryServiceLinkRepository categoryServices,
      SupportedServiceRepository supportedServices,
      GymEntranceHistoryRepository history,
      GymMembershipPolicyV4Repository policies,
      TranslationService translations,
      PlatformTransactionManager transactionManager) {
    this.categories = categories;
    this.gyms = gyms;
    this.gymPrices = gymPrices;
    this.categoryPrices = categoryPrices;
    this.categoryWorkHours = categoryWorkHours;
    this.categoryServices = categoryServices;
    this.supportedServices = supportedServices;
    this.history = history;
    this.policies = policies;
    this.translations = translations;
    this.tx = new TransactionTemplate(transactionManager);
  }

  static class DevSeedCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      String flag = context.getEnvironment().getProperty(PROPERTY);
      if (flag != null && !flag.isBlank()) {
        return Boolean.parseBoolean(flag.trim());
      }
      return context.getEnvironment().acceptsProfiles(Profiles.of("dev", "development", "local"));
    }
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      seed();
    } catch (Exception e) {
      log.warn("DEV category-scenario seeding failed; continuing startup", e);
    }
  }

  void seed() {
    List<Category> cats = categories.findAll(PageRequest.of(0, 8)).getContent();
    if (cats.isEmpty()) {
      log.warn("DEV category-scenario seeder skipped: categories table is empty");
      return;
    }
    Set<String> existing =
        gyms.findAll((root, q, cb) -> cb.like(root.get("name"), PREFIX + "%")).stream()
            .map(Gym::getName)
            .collect(Collectors.toSet());
    List<SupportedService> services =
        supportedServices.findAll(PageRequest.of(0, 5)).getContent();

    List<String> created = new ArrayList<>();
    List<String> skipped = new ArrayList<>();

    tx.executeWithoutResult(
        s -> {
          for (Category c : cats) {
            ensureNameTranslations("CATEGORY", c.getId().toString(), c.getName());
          }
          for (SupportedService service : services) {
            ensureNameTranslations(
                "SUPPORTEDSERVICE", service.getId().toString(), service.getName());
          }
        });

    run(FULLY_FILLED, 3, cats, existing, created, skipped, () -> seedFullyFilled(cats, services));
    run(MAIN_ONLY, 1, cats, existing, created, skipped, () -> seedMainOnly(cats));
    run(MULTI_SUB, 3, cats, existing, created, skipped, () -> seedMultiSub(cats));

    tx.executeWithoutResult(
        s ->
            gyms.findAll((root, q, cb) -> cb.equal(root.get("name"), FULLY_FILLED)).stream()
                .findFirst()
                .ifPresent(
                    g -> {
                      if (!history.existsByGymId(g.getId()) && seedHistory(g, cats)) {
                        created.add("history on '" + FULLY_FILLED + "'");
                      }
                    }));

    log.info(
        "DEV category-scenario seeder done: created={}, skipped={}", created, skipped);
  }

  private void ensureNameTranslations(String entityType, String entityId, String azName) {
    if (azName == null || azName.isBlank()) {
      return;
    }
    String[] enRu =
        NAME_I18N.getOrDefault(
            azName.trim().toLowerCase(Locale.ROOT), new String[] {azName, azName});
    upsert(entityType, entityId, "AZ", azName);
    upsert(entityType, entityId, "EN", enRu[0]);
    upsert(entityType, entityId, "RU", enRu[1]);
  }

  private void upsert(String entityType, String entityId, String lang, String value) {
    translations.saveOrUpdateTranslation(
        new CreateTranslationRequest(entityType, entityId, lang, "name", value));
  }

  private void run(
      String name,
      int categoriesNeeded,
      List<Category> cats,
      Set<String> existing,
      List<String> created,
      List<String> skipped,
      Runnable scenario) {
    if (existing.contains(name)) {
      skipped.add(name + " (exists)");
      return;
    }
    if (cats.size() < categoriesNeeded) {
      log.warn(
          "DEV scenario '{}' needs {} categories but only {} exist; skipped",
          name,
          categoriesNeeded,
          cats.size());
      skipped.add(name + " (needs " + categoriesNeeded + " categories)");
      return;
    }
    tx.executeWithoutResult(s -> scenario.run());
    created.add(name);
  }

  /** One complete gym: policy, packages, subs, hours, services, descriptions, QR, primary. */
  private void seedFullyFilled(List<Category> cats, List<SupportedService> services) {
    Category main = cats.get(0);
    List<Category> subs =
        cats.size() >= 3 ? List.of(cats.get(1), cats.get(2)) : List.of(cats.get(1));
    Gym gym =
        newGym(
            FULLY_FILLED,
            "fully-filled",
            40.3835,
            49.8560,
            Set.of(main),
            new LinkedHashSet<>(subs),
            true);
    gym.setPhone("+994501112233");
    gym.setEmail("dev-fully-filled@fitnest.az");
    gym.setCoverImageUrl("https://fitnest.az/static/dev/fully-filled-cover.jpg");
    gym.setHomeEligible(true);
    gym.setIsReservationEnabled(true);
    gym.setRating(4.7);
    gym.setReviewsCount(12);
    gym.setIsNew(true);
    gym.setGeneralWorkHours(weekHours(LocalTime.of(8, 0), LocalTime.of(22, 0)));
    gym = gyms.save(gym);

    attachSubscriptions(gym);
    writeMembershipPolicy(gym);
    for (int i = 0; i < PACKAGE_IDS.length; i++) {
      addGymPrice(gym, PACKAGE_IDS[i], 10.0 + i * 2, 80.0 + i * 20, true);
    }

    addDescription(gym, main, "Main fitness floor for DEV fully-filled gym.");
    for (int s = 0; s < subs.size(); s++) {
      Category sub = subs.get(s);
      addDescription(gym, sub, "Subcategory " + sub.getName() + " studio (single-entry priced).");
      for (int i = 0; i < PACKAGE_IDS.length; i++) {
        // Subcategory offerings are single-entry only (v6); monthly stays on membership.
        addCategoryPrice(gym, sub.getId(), PACKAGE_IDS[i], 18.0 + s * 3 + i, null);
      }
      for (GymWorkHourPeriod day : GymWorkHourPeriod.values()) {
        categoryWorkHours.save(
            GymCategoryWorkHour.builder()
                .gymId(gym.getId())
                .categoryId(sub.getId())
                .period(day)
                .fromTime(LocalTime.of(10, 0))
                .toTime(LocalTime.of(20, 0))
                .build());
      }
      for (SupportedService service : services.stream().limit(3).toList()) {
        categoryServices.save(
            GymCategoryServiceLink.builder()
                .gymId(gym.getId())
                .categoryId(sub.getId())
                .supportedServiceId(service.getId())
                .build());
      }
    }
  }

  private void seedMainOnly(List<Category> cats) {
    Gym gym = newGym(MAIN_ONLY, "main-only", 40.3790, 49.8480, Set.of(cats.get(0)), Set.of(), false);
    gym.setPhone("+994509990001");
    gym.setEmail("dev-main-only@fitnest.az");
    gym.setGeneralWorkHours(weekHours(LocalTime.of(7, 0), LocalTime.of(23, 0)));
    gym = gyms.save(gym);
    addGymPrice(gym, PACKAGE_IDS[0], 10.0, 80.0, false);
  }

  private void seedMultiSub(List<Category> cats) {
    Gym gym =
        newGym(
            MULTI_SUB,
            "multi-sub",
            40.3810,
            49.8520,
            Set.of(cats.get(0)),
            cats.size() > 2 ? Set.of(cats.get(1), cats.get(2)) : Set.of(cats.get(1)),
            true);
    gym.setPhone("+994509990002");
    addGymPrice(gym, PACKAGE_IDS[0], 10.0, 80.0, false);
    // First sub priced; second left empty → CATEGORY_PRICE_NOT_CONFIGURED for that category.
    addCategoryPrice(gym, cats.get(1).getId(), PACKAGE_IDS[0], 20.0, null);
  }

  private boolean seedHistory(Gym gym, List<Category> cats) {
    Long mainId = cats.get(0).getId();
    Long subId = cats.size() > 1 ? cats.get(1).getId() : null;
    if (subId == null) {
      return false;
    }
    LocalDateTime now = LocalDateTime.now(BAKU);
    history.save(historyRow(gym, now.minusDays(2), null, 10.0, "ELIGIBLE"));
    history.save(historyRow(gym, now.minusDays(1), mainId, 10.0, "ELIGIBLE"));
    history.save(historyRow(gym, now.minusHours(3), subId, 18.0, "ELIGIBLE"));
    history.save(historyRow(gym, now.minusHours(5), subId, 0.0, "UNSUCCESSFUL"));
    return true;
  }

  private GymEntranceHistory historyRow(
      Gym gym, LocalDateTime at, Long categoryId, double amount, String status) {
    return GymEntranceHistory.builder()
        .userId(DEV_USER_ID)
        .gymId(gym.getId())
        .scanDate(at)
        .status(status)
        .platform("DEV_SEED")
        .amount(amount)
        .packageId(PACKAGE_IDS[0])
        .categoryId(categoryId)
        .build();
  }

  private Gym newGym(
      String name,
      String slug,
      double lat,
      double lng,
      Set<Category> mains,
      Set<Category> subs,
      boolean hasSubs) {
    Gym gym =
        Gym.builder()
            .name(name)
            .description(
                "Synthetic DEV scenario gym ("
                    + slug
                    + "). Fully wired for admin/mobile QA. Safe to delete.")
            .address(new Address("DEV scenario, Baku", "Baku", lat, lng))
            .status(GymStatus.ACTIVE)
            .hasSubcategories(hasSubs)
            .mainCategories(new LinkedHashSet<>(mains))
            .subCategories(new LinkedHashSet<>(subs))
            .build();
    gym = gyms.save(gym);
    gym.setQrCodeValue("https://fitnest.az/gym/" + gym.getId());
    gym.setQrCodeToken("DEV-SCENARIO-" + slug.toUpperCase(Locale.ROOT) + "-" + gym.getId());
    return gyms.save(gym);
  }

  private void attachSubscriptions(Gym gym) {
    for (int i = 0; i < PACKAGE_IDS.length; i++) {
      long packageId = PACKAGE_IDS[i];
      boolean present =
          gym.getSubscriptions().stream().anyMatch(s -> packageId == s.getPackageId());
      if (present) {
        continue;
      }
      GymSubscription sub = new GymSubscription();
      sub.setGym(gym);
      sub.setPackageId(packageId);
      sub.setDailyPrice(10.0 + i * 2);
      sub.setSupportedServices(new HashSet<>());
      gym.getSubscriptions().add(sub);
    }
    gyms.save(gym);
  }

  private void writeMembershipPolicy(Gym gym) {
    List<PackageRule> rules = new ArrayList<>();
    for (int i = 0; i < PACKAGE_IDS.length; i++) {
      rules.add(
          new PackageRule(
              PACKAGE_IDS[i],
              PACKAGE_TIERS[i],
              true,
              true,
              BigDecimal.valueOf(10.0 + i * 2).setScale(2),
              BigDecimal.valueOf(80.0 + i * 20).setScale(2),
              List.of(),
              null));
    }
    GymMembershipPolicyV4 payload = new GymMembershipPolicyV4(0L, false, rules, List.of());
    try {
      GymMembershipPolicyV4Entity row =
          policies.findById(gym.getId()).orElseGet(GymMembershipPolicyV4Entity::new);
      row.setGymId(gym.getId());
      row.setPayload(mapper.writeValueAsString(payload));
      policies.save(row);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to write DEV membership policy", e);
    }
  }

  private void addDescription(Gym gym, Category category, String text) {
    gym.getDescriptions()
        .add(
            GymDescription.builder()
                .gym(gym)
                .category(category)
                .description(text)
                .phone(gym.getPhone())
                .coverImageUrl(
                    "https://fitnest.az/static/dev/cat-" + category.getId() + ".jpg")
                .build());
    gyms.save(gym);
  }

  private Set<GymWorkHour> weekHours(LocalTime from, LocalTime to) {
    Set<GymWorkHour> hours = new LinkedHashSet<>();
    for (GymWorkHourPeriod day : EnumSet.allOf(GymWorkHourPeriod.class)) {
      hours.add(GymWorkHour.builder().period(day).fromTime(from).toTime(to).build());
    }
    return hours;
  }

  private void addGymPrice(
      Gym gym, long packageId, double single, double monthly, boolean homeEligible) {
    gymPrices.save(
        GymPackagePrice.builder()
            .gym(gym)
            .packageId(packageId)
            .included(true)
            .homeEligible(homeEligible)
            .singlePrice(single)
            .unlimitedPrice(monthly)
            .effectiveFrom(PRICE_FROM)
            .build());
  }

  private void addCategoryPrice(
      Gym gym, Long categoryId, long packageId, double single, Double monthly) {
    categoryPrices.save(
        GymCategoryPackagePrice.builder()
            .gym(gym)
            .categoryId(categoryId)
            .packageId(packageId)
            .singlePrice(single)
            .unlimitedPrice(monthly)
            .effectiveFrom(PRICE_FROM)
            .build());
  }
}

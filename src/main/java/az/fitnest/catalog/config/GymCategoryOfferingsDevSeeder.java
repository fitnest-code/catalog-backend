package az.fitnest.catalog.config;

import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymCategoryPackagePrice;
import az.fitnest.catalog.model.entity.GymCategoryServiceLink;
import az.fitnest.catalog.model.entity.GymCategoryWorkHour;
import az.fitnest.catalog.model.entity.GymEntranceHistory;
import az.fitnest.catalog.model.entity.GymPackagePrice;
import az.fitnest.catalog.model.entity.SupportedService;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.model.enums.GymWorkHourPeriod;
import az.fitnest.catalog.repository.CategoryRepository;
import az.fitnest.catalog.repository.GymCategoryPackagePriceRepository;
import az.fitnest.catalog.repository.GymCategoryServiceLinkRepository;
import az.fitnest.catalog.repository.GymCategoryWorkHourRepository;
import az.fitnest.catalog.repository.GymEntranceHistoryRepository;
import az.fitnest.catalog.repository.GymPackagePriceRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.SupportedServiceRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
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
 * DEV-only mock data for gym subcategory offerings (v5). Creates four synthetic gyms named
 * {@code [DEV-SCENARIO] ...} so QA/ops can exercise every category edge case without touching real gyms:
 *
 * <ol>
 *   <li>Main-only: no sub rows, only gym_package_price -> a scan with null categoryId must keep working.</li>
 *   <li>Main+Sub priced: sub has gym_category_package_price, category work hours and a service link.</li>
 *   <li>Multi-Sub Mixed: one priced sub and one sub deliberately WITHOUT price -> CATEGORY_PRICE_NOT_CONFIGURED.</li>
 *   <li>Dual-Main No Sub: two mains, hasSubcategories=false -> picker still required.</li>
 * </ol>
 * Plus three gym_entrance_history rows on scenario 2 (category null / main / sub) so analytics by-category
 * shows the legacy bucket next to the split ones.
 *
 * <p><b>Activation</b> (never active on prod unless explicitly forced):
 * <ul>
 *   <li>Automatically when a Spring profile {@code dev}, {@code development} or {@code local} is active.</li>
 *   <li>Force on any profile (e.g. a DEV k8s overlay whose profile has another name):
 *       {@code fitnest.dev.seed-category-scenarios=true} / env {@code FITNEST_DEV_SEED_CATEGORY_SCENARIOS=true}.</li>
 *   <li>Opt out even on a dev profile: {@code fitnest.dev.seed-category-scenarios=false}.</li>
 * </ul>
 * Idempotent: a scenario is skipped when a gym with its exact name already exists, and the history rows are
 * only written while the Main+Sub gym has none. Delete the gyms by name to re-seed. Categories are reused from
 * the DB (never invented); scenarios that need more categories than exist are skipped with a warning.
 */
@Slf4j
@Component
@Conditional(GymCategoryOfferingsDevSeeder.DevSeedCondition.class)
public class GymCategoryOfferingsDevSeeder implements ApplicationRunner {

    public static final String PROPERTY = "fitnest.dev.seed-category-scenarios";
    public static final String PREFIX = "[DEV-SCENARIO] ";
    public static final String MAIN_ONLY = PREFIX + "Main-only Gym";
    public static final String MAIN_SUB = PREFIX + "Main+Sub Priced";
    public static final String MULTI_SUB = PREFIX + "Multi-Sub Mixed";
    public static final String DUAL_MAIN = PREFIX + "Dual-Main No Sub";

    /** Bronze, same id HomeGymRuleServiceTest uses; package catalog itself lives in order-backend. */
    static final long PACKAGE_ID = 1201L;
    /** Synthetic user id for history rows; entrance history has no FK to users (separate service). */
    static final long DEV_USER_ID = 900_000_001L;
    private static final ZoneId BAKU = ZoneId.of("Asia/Baku");

    private final CategoryRepository categories;
    private final GymRepository gyms;
    private final GymPackagePriceRepository gymPrices;
    private final GymCategoryPackagePriceRepository categoryPrices;
    private final GymCategoryWorkHourRepository categoryWorkHours;
    private final GymCategoryServiceLinkRepository categoryServices;
    private final SupportedServiceRepository supportedServices;
    private final GymEntranceHistoryRepository history;
    private final TransactionTemplate tx;

    public GymCategoryOfferingsDevSeeder(
            CategoryRepository categories,
            GymRepository gyms,
            GymPackagePriceRepository gymPrices,
            GymCategoryPackagePriceRepository categoryPrices,
            GymCategoryWorkHourRepository categoryWorkHours,
            GymCategoryServiceLinkRepository categoryServices,
            SupportedServiceRepository supportedServices,
            GymEntranceHistoryRepository history,
            PlatformTransactionManager transactionManager) {
        this.categories = categories;
        this.gyms = gyms;
        this.gymPrices = gymPrices;
        this.categoryPrices = categoryPrices;
        this.categoryWorkHours = categoryWorkHours;
        this.categoryServices = categoryServices;
        this.supportedServices = supportedServices;
        this.history = history;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /**
     * Profile OR property gate. A plain {@code @Profile} + {@code @ConditionalOnProperty} pair would be an AND,
     * which cannot force-enable on a DEV cluster whose profile name is not one of ours.
     */
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
        // Seed data must never prevent the service from starting.
        try {
            seed();
        } catch (Exception e) {
            log.warn("DEV category-scenario seeding failed; continuing startup", e);
        }
    }

    void seed() {
        List<Category> cats = categories.findAll(PageRequest.of(0, 3)).getContent();
        if (cats.isEmpty()) {
            log.warn("DEV category-scenario seeder skipped: categories table is empty");
            return;
        }
        Set<String> existing = gyms
                .findAll((root, q, cb) -> cb.like(root.get("name"), PREFIX + "%"))
                .stream()
                .map(Gym::getName)
                .collect(Collectors.toSet());
        SupportedService service = supportedServices.findAll(PageRequest.of(0, 1)).stream().findFirst().orElse(null);

        List<String> created = new java.util.ArrayList<>();
        List<String> skipped = new java.util.ArrayList<>();

        run(MAIN_ONLY, 1, cats, existing, created, skipped, () -> seedMainOnly(cats));
        run(MAIN_SUB, 2, cats, existing, created, skipped, () -> seedMainSub(cats, service));
        run(MULTI_SUB, 3, cats, existing, created, skipped, () -> seedMultiSub(cats));
        run(DUAL_MAIN, 2, cats, existing, created, skipped, () -> seedDualMain(cats));

        // History is keyed to the Main+Sub gym, so it is also repaired if that gym exists without rows.
        tx.executeWithoutResult(s -> gyms
                .findAll((root, q, cb) -> cb.equal(root.get("name"), MAIN_SUB))
                .stream().findFirst().ifPresent(g -> {
                    if (!history.existsByGymId(g.getId()) && seedHistory(g, cats)) {
                        created.add("history x3 on '" + MAIN_SUB + "'");
                    }
                }));

        if (service == null) {
            log.warn("DEV category-scenario seeder: no supported_services row found, service links skipped");
        }
        log.info("DEV category-scenario seeder done: created={}, skipped(existing or not enough categories)={}",
                created, skipped);
    }

    private void run(String name, int categoriesNeeded, List<Category> cats, Set<String> existing,
                     List<String> created, List<String> skipped, Runnable scenario) {
        if (existing.contains(name)) {
            skipped.add(name + " (exists)");
            return;
        }
        if (cats.size() < categoriesNeeded) {
            log.warn("DEV scenario '{}' needs {} categories but only {} exist; skipped", name, categoriesNeeded,
                    cats.size());
            skipped.add(name + " (needs " + categoriesNeeded + " categories)");
            return;
        }
        tx.executeWithoutResult(s -> scenario.run());
        created.add(name);
    }

    private void seedMainOnly(List<Category> cats) {
        Gym gym = newGym(MAIN_ONLY, "main-only", 40.3790, 49.8480, Set.of(cats.get(0)), Set.of(), false);
        addGymPrice(gym, 10.0, 80.0);
    }

    private void seedMainSub(List<Category> cats, SupportedService service) {
        Category main = cats.get(0);
        Category sub = cats.get(1);
        Gym gym = newGym(MAIN_SUB, "main-sub", 40.3800, 49.8500, Set.of(main), Set.of(sub), true);
        addGymPrice(gym, 10.0, 80.0);
        addCategoryPrice(gym, sub.getId(), 18.0, 140.0);
        // Sub-specific hours differ from gym general hours so the "category hours win" path is visible.
        for (GymWorkHourPeriod day : List.of(GymWorkHourPeriod.MONDAY, GymWorkHourPeriod.WEDNESDAY,
                GymWorkHourPeriod.FRIDAY)) {
            categoryWorkHours.save(GymCategoryWorkHour.builder().gymId(gym.getId()).categoryId(sub.getId())
                    .period(day).fromTime(LocalTime.of(10, 0)).toTime(LocalTime.of(20, 0)).build());
        }
        if (service != null) {
            categoryServices.save(GymCategoryServiceLink.builder().gymId(gym.getId()).categoryId(sub.getId())
                    .supportedServiceId(service.getId()).build());
        }
    }

    private void seedMultiSub(List<Category> cats) {
        Gym gym = newGym(MULTI_SUB, "multi-sub", 40.3810, 49.8520, Set.of(cats.get(0)),
                cats.size() > 2 ? Set.of(cats.get(1), cats.get(2)) : Set.of(cats.get(1)), true);
        addGymPrice(gym, 10.0, 80.0);
        // cats.get(1) priced; cats.get(2) intentionally has NO row -> CATEGORY_PRICE_NOT_CONFIGURED in DEV.
        addCategoryPrice(gym, cats.get(1).getId(), 20.0, 150.0);
    }

    private void seedDualMain(List<Category> cats) {
        Gym gym = newGym(DUAL_MAIN, "dual-main", 40.3820, 49.8540, Set.of(cats.get(0), cats.get(1)), Set.of(), false);
        addGymPrice(gym, 12.0, 95.0);
    }

    /** @return true when rows were written */
    private boolean seedHistory(Gym gym, List<Category> cats) {
        Long mainId = cats.get(0).getId();
        Long subId = cats.size() > 1 ? cats.get(1).getId() : null;
        if (subId == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now(BAKU);
        // null = legacy scan from before the category picker; keeps the "unassigned" analytics bucket alive.
        history.save(historyRow(gym, now.minusDays(2), null, 10.0));
        history.save(historyRow(gym, now.minusDays(1), mainId, 10.0));
        history.save(historyRow(gym, now.minusHours(3), subId, 18.0));
        return true;
    }

    private GymEntranceHistory historyRow(Gym gym, LocalDateTime at, Long categoryId, double amount) {
        return GymEntranceHistory.builder()
                .userId(DEV_USER_ID).gymId(gym.getId()).scanDate(at).status("ELIGIBLE")
                .platform("DEV_SEED").amount(amount).packageId(PACKAGE_ID).categoryId(categoryId).build();
    }

    private Gym newGym(String name, String slug, double lat, double lng, Set<Category> mains, Set<Category> subs,
                       boolean hasSubs) {
        Gym gym = Gym.builder()
                .name(name)
                .description("Synthetic DEV scenario gym (" + slug + "). Safe to delete.")
                .address(new Address("DEV scenario, Baku", "Baku", lat, lng))
                .status(GymStatus.ACTIVE)
                .hasSubcategories(hasSubs)
                .mainCategories(new LinkedHashSet<>(mains))
                .subCategories(new LinkedHashSet<>(subs))
                .build();
        gym = gyms.save(gym);
        // Scan flow resolves the gym from ".../gym/{id}" (see GymEntranceServiceImpl.extractGymIdFromQr),
        // so the id has to exist before the QR value can be set. Token is unique per scenario.
        gym.setQrCodeValue("https://fitnest.az/gym/" + gym.getId());
        gym.setQrCodeToken("DEV-SCENARIO-" + slug.toUpperCase() + "-" + gym.getId());
        return gyms.save(gym);
    }

    private void addGymPrice(Gym gym, double single, double monthly) {
        gymPrices.save(GymPackagePrice.builder().gym(gym).packageId(PACKAGE_ID).included(true)
                .singlePrice(single).unlimitedPrice(monthly).effectiveFrom(LocalDate.of(2026, 1, 1)).build());
    }

    private void addCategoryPrice(Gym gym, Long categoryId, double single, double monthly) {
        categoryPrices.save(GymCategoryPackagePrice.builder().gym(gym).categoryId(categoryId).packageId(PACKAGE_ID)
                .singlePrice(single).unlimitedPrice(monthly).effectiveFrom(LocalDate.of(2026, 1, 1)).build());
    }
}

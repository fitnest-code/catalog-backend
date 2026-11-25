package az.fitnest.catalog.service.category;

import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymCategoryPackagePrice;
import az.fitnest.catalog.model.entity.GymPackagePrice;
import az.fitnest.catalog.repository.GymCategoryPackagePriceRepository;
import az.fitnest.catalog.repository.GymPackagePriceRepository;
import az.fitnest.catalog.service.homegym.HomeGymRuleService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolves single/monthly entry prices for a gym category. Main-category (or null) uses
 * existing {@code gym_package_price}; subcategory uses {@code gym_category_package_price}.
 */
@Component
@RequiredArgsConstructor
public class CategoryEntryPriceResolver {

    private static final ZoneId BAKU = ZoneId.of("Asia/Baku");

    private final GymCategoryPackagePriceRepository categoryPrices;
    private final GymPackagePriceRepository gymPrices;

    public record Prices(Double singlePrice, Double monthlyPrice, Long categoryId, boolean fromSubcategoryTable) {}

    /** All category ids the gym may offer at QR (main ∪ sub). */
    public static Set<Long> offeredCategoryIds(Gym gym) {
        Set<Long> ids = new HashSet<>();
        if (gym.getMainCategories() != null) {
            for (Category c : gym.getMainCategories()) {
                if (c != null && c.getCategoryId() != null) ids.add(c.getCategoryId());
            }
        }
        if (gym.getSubCategories() != null) {
            for (Category c : gym.getSubCategories()) {
                if (c != null && c.getCategoryId() != null) ids.add(c.getCategoryId());
            }
        }
        return ids;
    }

    public static Set<Long> subCategoryIds(Gym gym) {
        Set<Long> ids = new HashSet<>();
        if (gym.getSubCategories() != null) {
            for (Category c : gym.getSubCategories()) {
                if (c != null && c.getCategoryId() != null) ids.add(c.getCategoryId());
            }
        }
        return ids;
    }

    public void requireOwnedCategory(Gym gym, Long categoryId) {
        if (categoryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CATEGORY_ID_REQUIRED");
        }
        if (!offeredCategoryIds(gym).contains(categoryId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CATEGORY_NOT_ON_GYM");
        }
    }

    /**
     * @param categoryId required when gym has subcategories; for main-only gyms may be null → gym price
     */
    public Prices resolve(Gym gym, Long packageId, Long categoryId) {
        Objects.requireNonNull(gym, "gym");
        Objects.requireNonNull(packageId, "packageId");
        LocalDate today = LocalDate.now(BAKU);
        Set<Long> subs = subCategoryIds(gym);
        boolean hasSubs = !subs.isEmpty() || Boolean.TRUE.equals(gym.getHasSubcategories());

        if (hasSubs) {
            requireOwnedCategory(gym, categoryId);
        } else if (categoryId != null) {
            requireOwnedCategory(gym, categoryId);
        }

        Long effectiveCategory = categoryId;
        if (effectiveCategory == null && gym.getMainCategories() != null && !gym.getMainCategories().isEmpty()) {
            Category first = gym.getMainCategories().iterator().next();
            effectiveCategory = first != null ? first.getCategoryId() : null;
        }

        if (effectiveCategory != null && subs.contains(effectiveCategory)) {
            Optional<GymCategoryPackagePrice> row =
                    categoryPrices
                            .findFirstByGymIdAndCategoryIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                                    gym.getId(), effectiveCategory, packageId, today);
            if (row.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CATEGORY_PRICE_NOT_CONFIGURED");
            }
            GymCategoryPackagePrice p = row.get();
            return new Prices(nz(p.getSinglePrice()), nz(p.getUnlimitedPrice()), effectiveCategory, true);
        }

        // Main category / legacy: gym_package_price (same source as HomeGymRuleService).
        Optional<GymPackagePrice> main =
                gymPrices.findFirstByGymIdAndPackageIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                        gym.getId(), packageId, today);
        if (main.isPresent() && Boolean.TRUE.equals(main.get().getIncluded())) {
            GymPackagePrice p = main.get();
            return new Prices(nz(p.getSinglePrice()), nz(p.getUnlimitedPrice()), effectiveCategory, false);
        }
        // Fallback daily_price from subscriptions (same as home-gym).
        if (gym.getSubscriptions() != null) {
            for (var sub : gym.getSubscriptions()) {
                if (sub != null && packageId.equals(sub.getPackageId()) && sub.getDailyPrice() != null) {
                    return new Prices(sub.getDailyPrice(), null, effectiveCategory, false);
                }
            }
        }
        return new Prices(0.0, 0.0, effectiveCategory, false);
    }

    private static Double nz(Double v) {
        return v == null ? 0.0 : v;
    }
}

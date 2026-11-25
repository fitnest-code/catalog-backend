package az.fitnest.catalog.service.category;

import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.Gym;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tiny builders shared by the v5 category tests. Categories/gyms are plain objects (no JPA): ids come from
 * BaseEntity setters. LinkedHashSet keeps "first main category" deterministic, mirroring what the resolver
 * relies on when categoryId is null.
 */
public final class CategoryTestFixtures {

    public static final Long GYM_ID = 501L;
    public static final Long GYM_MAIN = 11L;
    public static final Long GYM_MAIN_2 = 12L;
    public static final Long GYM_SUB = 21L;
    public static final Long GYM_SUB_2 = 22L;
    public static final Long BRONZE = 1201L;

    private CategoryTestFixtures() {}

    public static Category category(Long id, String name) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        return c;
    }

    public static Gym gym(List<Category> mains, List<Category> subs, boolean hasSubcategories) {
        Gym gym = new Gym();
        gym.setId(GYM_ID);
        gym.setName("Test gym");
        gym.setMainCategories(new LinkedHashSet<>(mains));
        gym.setSubCategories(new LinkedHashSet<>(subs));
        gym.setHasSubcategories(hasSubcategories);
        return gym;
    }

    public static Set<Long> ids(Long... ids) {
        return new LinkedHashSet<>(List.of(ids));
    }
}

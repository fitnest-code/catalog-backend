package az.fitnest.catalog.service;

import static az.fitnest.catalog.service.category.CategoryTestFixtures.GYM_MAIN;
import static az.fitnest.catalog.service.category.CategoryTestFixtures.GYM_SUB;
import static az.fitnest.catalog.service.category.CategoryTestFixtures.category;
import static az.fitnest.catalog.service.category.CategoryTestFixtures.gym;
import static org.junit.jupiter.api.Assertions.assertEquals;

import az.fitnest.catalog.model.entity.Gym;
import java.util.List;
import org.junit.jupiter.api.Test;

class GymEntranceV6ServiceTest {

  @Test
  void resolveRole_defaultsToMainWithoutCategory() {
    Gym g = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(category(GYM_SUB, "Crossfit")), true);
    assertEquals("MAIN", GymEntranceV6Service.resolveRole(g, null));
  }

  @Test
  void resolveRole_marksSubWhenOfferingIsSub() {
    Gym g = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(category(GYM_SUB, "Crossfit")), true);
    assertEquals("SUB", GymEntranceV6Service.resolveRole(g, GYM_SUB));
  }

  @Test
  void resolveRole_marksMainWhenCategoryIsMain() {
    Gym g = gym(List.of(category(GYM_MAIN, "Fitness")), List.of(category(GYM_SUB, "Crossfit")), true);
    assertEquals("MAIN", GymEntranceV6Service.resolveRole(g, GYM_MAIN));
  }
}

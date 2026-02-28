package az.fitnest.catalog.configuration;

import az.fitnest.catalog.model.entity.*;
import az.fitnest.catalog.model.enums.GymStatus;
import az.fitnest.catalog.model.enums.StoreStatus;
import az.fitnest.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final CategoryRepository categoryRepository;
    private final ProfessionRepository professionRepository;
    private final GymRepository gymRepository;
    private final StoreRepository storeRepository;
    private final TrainerRepository trainerRepository;

    @Bean
    public CommandLineRunner initCatalogData() {
        return args -> {
            initCategories();
            initProfessions();
            initGyms();
            initStores();
        };
    }

    private void initCategories() {
        createCategoryIfNotFound("Fitness");
        createCategoryIfNotFound("Yoga");
        createCategoryIfNotFound("Boxing");
        createCategoryIfNotFound("Swimming");
        createCategoryIfNotFound("Crossfit");
    }

    private void createCategoryIfNotFound(String name) {
        if (!categoryRepository.existsByName(name)) {
            Category category = new Category();
            category.setName(name);
            categoryRepository.save(category);
        }
    }

    private void initProfessions() {
        createProfessionIfNotFound("Fitness Trainer");
        createProfessionIfNotFound("Yoga Instructor");
        createProfessionIfNotFound("Boxing Coach");
        createProfessionIfNotFound("Nutritionist");
    }

    private void createProfessionIfNotFound(String name) {
        // Assuming ProfessionRepository has existsByName or similar, checking findByName
        if (professionRepository.findAll().stream().noneMatch(p -> p.getName().equals(name))) {
            Profession profession = new Profession();
            profession.setName(name);
            professionRepository.save(profession);
        }
    }

    private void initGyms() {
        if (gymRepository.count() == 0) {
            Category fitnessCategory = categoryRepository.findAll().stream()
                    .filter(c -> c.getName().equals("Fitness"))
                    .findFirst()
                    .orElse(null);

            Address address = Address.builder()
                    .addressText("123 Main St")
                    .city("Baku")
                    .latitude(40.4093)
                    .longitude(49.8671)
                    .build();

            Gym gym = new Gym();
            gym.setName("Premium Fitness Center");
            gym.setDescription("High-quality fitness center with modern equipment and professional trainers");
            gym.setAddress(address);
            gym.setPhone("+994500000000");
            gym.setEmail("info@premiumfitness.az");
            gym.setStatus(GymStatus.ACTIVE);
            gym.setRating(4.5);
            gym.setReviewsCount(10);
            gym.setIsNew(true);
            
            if (fitnessCategory != null) {
                gym.setCategories(new HashSet<>(Collections.singletonList(fitnessCategory)));
            }

            gymRepository.save(gym);
        }
    }

    private void initStores() {
        if (storeRepository.count() == 0) {
            StoreAddress address = new StoreAddress("456 Market St", "Baku", 40.4095, 49.8675);

            Store store = new Store();
            store.setName("Sports World");
            store.setCategory("Equipment");
            store.setStatus(StoreStatus.ACTIVE.name());
            store.setAddress(address);
            store.setPhone("+994510000000");
            store.setPopularScore(9.8);

            storeRepository.save(store);
        }
    }
}

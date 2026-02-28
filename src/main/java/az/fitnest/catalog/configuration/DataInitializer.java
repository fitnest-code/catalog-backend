package az.fitnest.catalog.configuration;

import az.fitnest.catalog.model.entity.*;
import java.util.Optional;
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
    private final TranslationRepository translationRepository;

    @Bean
    public CommandLineRunner initCatalogData() {
        return args -> {
            initCategories();
            initProfessions();
            initGyms();
            initStores();
            initTrainers();
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
            category = categoryRepository.save(category);
            
            // Seed translations (assuming original is EN)
            createTranslationIfNotFound("Category", category.getCategoryId().toString(), "AZ", "name", translateCategoryToAz(name));
            createTranslationIfNotFound("Category", category.getCategoryId().toString(), "RU", "name", translateCategoryToRu(name));
        }
    }

    private String translateCategoryToAz(String name) {
        return switch (name) {
            case "Fitness" -> "Fitnes";
            case "Yoga" -> "Yoqa";
            case "Boxing" -> "Boks";
            case "Swimming" -> "Üzgüçülük";
            case "Crossfit" -> "Krossfit";
            default -> name;
        };
    }

    private String translateCategoryToRu(String name) {
        return switch (name) {
            case "Fitness" -> "Фитнес";
            case "Yoga" -> "Йога";
            case "Boxing" -> "Бокс";
            case "Swimming" -> "Плавание";
            case "Crossfit" -> "Кроссфит";
            default -> name;
        };
    }

    private void initProfessions() {
        createProfessionIfNotFound("Fitness Trainer");
        createProfessionIfNotFound("Yoga Instructor");
        createProfessionIfNotFound("Boxing Coach");
        createProfessionIfNotFound("Nutritionist");
    }

    private void createProfessionIfNotFound(String name) {
        Optional<Profession> existing = professionRepository.findAll().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst();
        
        if (existing.isEmpty()) {
            Profession profession = new Profession();
            profession.setName(name);
            profession = professionRepository.save(profession);
            
            createTranslationIfNotFound("Profession", profession.getProfessionId().toString(), "AZ", "name", translateProfessionToAz(name));
            createTranslationIfNotFound("Profession", profession.getProfessionId().toString(), "RU", "name", translateProfessionToRu(name));
        }
    }

    private String translateProfessionToAz(String name) {
        return switch (name) {
            case "Fitness Trainer" -> "Fitnes Məşqçisi";
            case "Yoga Instructor" -> "Yoqa Təlimatçısı";
            case "Boxing Coach" -> "Boks Məşqçisi";
            case "Nutritionist" -> "Nutrisioloq";
            default -> name;
        };
    }

    private String translateProfessionToRu(String name) {
        return switch (name) {
            case "Fitness Trainer" -> "Фитнес-тренер";
            case "Yoga Instructor" -> "Инструктор по йоге";
            case "Boxing Coach" -> "Тренер по боксу";
            case "Nutritionist" -> "Нутрициолог";
            default -> name;
        };
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
            gym.setCoverImageUrl("https://picsum.photos/seed/gym1/800/600");
            gym.setLogoUrl("https://picsum.photos/seed/gymlogo1/200/200");
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

            gym = gymRepository.save(gym);

            String gymId = gym.getGymId().toString();
            createTranslationIfNotFound("Gym", gymId, "AZ", "name", "Premium Fitnes Mərkəzi");
            createTranslationIfNotFound("Gym", gymId, "RU", "name", "Премиум Фитнес Центр");
            createTranslationIfNotFound("Gym", gymId, "AZ", "description", "Müasir avadanlıq və peşəkar məşqçilər ilə yüksək keyfiyyətli fitnes mərkəzi");
            createTranslationIfNotFound("Gym", gymId, "RU", "description", "Высококачественный фитнес-центр с современным оборудованием и профессиональными тренерами");
        }
    }

    private void initStores() {
        if (storeRepository.count() == 0) {
            StoreAddress address = new StoreAddress("456 Market St", "Baku", 40.4095, 49.8675);

            Store store = new Store();
            store.setName("Sports World");
            store.setCategory("Equipment");
            store.setStatus(StoreStatus.ACTIVE.name());
            store.setCoverImageUrl("https://picsum.photos/seed/store1/800/600");
            store.setLogoUrl("https://picsum.photos/seed/storelogo1/200/200");
            store.setAddress(address);
            store.setPhone("+994510000000");
            store.setPopularScore(9.8);

            store = storeRepository.save(store);

            String storeId = store.getStoreId().toString();
            createTranslationIfNotFound("Store", storeId, "AZ", "name", "İdman Dünyası");
            createTranslationIfNotFound("Store", storeId, "RU", "name", "Мир Спорта");
            createTranslationIfNotFound("Store", storeId, "AZ", "category", "Avadanlıq");
            createTranslationIfNotFound("Store", storeId, "RU", "category", "Оборудование");
        }
    }

    private void initTrainers() {
        if (trainerRepository.count() == 0) {
            Trainer trainer = new Trainer();
            trainer.setFirstName("John");
            trainer.setLastName("Doe");
            trainer.setSpecialization("Fitness");
            trainer.setExperienceYears(10);
            trainer.setRating(4.9);
            trainer.setProfileImageUrl("https://i.pravatar.cc/150?u=trainer1");
            
            trainer = trainerRepository.save(trainer);
            
            String trainerId = trainer.getTrainerId().toString();
            createTranslationIfNotFound("Trainer", trainerId, "AZ", "specialization", "Fitnes");
            createTranslationIfNotFound("Trainer", trainerId, "RU", "specialization", "Фитнес");
        }
    }

    private void createTranslationIfNotFound(String entityType, String entityId, String languageCode, String fieldName, String fieldValue) {
        if (!translationRepository.existsByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(entityType, entityId, languageCode, fieldName)) {
            Translation translation = Translation.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .languageCode(languageCode)
                    .fieldName(fieldName)
                    .fieldValue(fieldValue)
                    .build();
            translationRepository.save(translation);
        }
    }
}

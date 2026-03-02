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
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;

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
            gym.setResponsiblePerson("Eli Quliyev");

            // Work Hours
            List<GymWorkHour> workHours = new ArrayList<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                workHours.add(new GymWorkHour(day, LocalTime.of(8, 0), LocalTime.of(22, 0)));
            }
            gym.setWorkHours(workHours);

            // Social Links
            List<GymSocialLink> socialLinks = new ArrayList<>();
            socialLinks.add(new GymSocialLink("Instagram", "https://instagram.com/premiumfitness"));
            socialLinks.add(new GymSocialLink("Facebook", "https://facebook.com/premiumfitness"));
            gym.setSocialLinks(socialLinks);

            // Images
            List<GymImage> images = new ArrayList<>();
            images.add(GymImage.builder()
                    .gym(gym)
                    .imageName("Main Hall")
                    .url("https://picsum.photos/seed/gym1/1200/800")
                    .type("interior")
                    .title("Main Training Hall")
                    .build());
            images.add(GymImage.builder()
                    .gym(gym)
                    .imageName("Pool")
                    .url("https://picsum.photos/seed/gympool1/1200/800")
                    .type("swimming_pool")
                    .title("Olympic Swimming Pool")
                    .build());
            gym.setImages(images);

            if (fitnessCategory != null) {
                gym.setCategories(new HashSet<>(Collections.singletonList(fitnessCategory)));
            }

            gym = gymRepository.save(gym);

            // Add Trainers
            Trainer trainer1 = new Trainer();
            trainer1.setFirstName("Eli");
            trainer1.setLastName("Memmedov");
            trainer1.setSpecialization("Fitness");
            trainer1.setExperienceYears(5);
            trainer1.setRating(4.8);
            trainer1.setProfileImageUrl("https://i.pravatar.cc/150?u=trainer1");

            Trainer trainer2 = new Trainer();
            trainer2.setFirstName("Lale");
            trainer2.setLastName("Resulova");
            trainer2.setSpecialization("Yoga");
            trainer2.setExperienceYears(3);
            trainer2.setRating(4.9);
            trainer2.setProfileImageUrl("https://i.pravatar.cc/150?u=trainer2");

            gym.getTrainers().add(trainer1);
            gym.getTrainers().add(trainer2);
            gymRepository.save(gym);

            String gymId = gym.getGymId().toString();
            createTranslationIfNotFound("Gym", gymId, "AZ", "name", "Premium Fitnes Mərkəzi");
            createTranslationIfNotFound("Gym", gymId, "RU", "name", "Премиум Фитнес Центр");
            createTranslationIfNotFound("Gym", gymId, "AZ", "description", "Müasir avadanlıq və peşəkar məşqçilər ilə yüksək keyfiyyətli fitnes mərkəzi");
            createTranslationIfNotFound("Gym", gymId, "RU", "description", "Высококачественный фитнес-центр с современным оборудованием и профессиональными тренерами");

            // Second Gym
            Address address2 = Address.builder()
                    .addressText("456 Secondary St")
                    .city("Baku")
                    .latitude(40.3850)
                    .longitude(49.8250)
                    .build();

            Gym gym2 = new Gym();
            gym2.setName("FitLife Studio");
            gym2.setDescription("Boutique fitness studio for focused workouts");
            gym2.setCoverImageUrl("https://picsum.photos/seed/gym2/800/600");
            gym2.setLogoUrl("https://picsum.photos/seed/gymlogo2/200/200");
            gym2.setAddress(address2);
            gym2.setPhone("+994500000001");
            gym2.setEmail("info@fitlifestudio.az");
            gym2.setStatus(GymStatus.ACTIVE);
            gym2.setRating(4.8);
            gym2.setReviewsCount(25);
            gym2.setIsNew(false);
            gym2.setResponsiblePerson("Aysel Memmedova");

            List<GymWorkHour> workHours2 = new ArrayList<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                workHours2.add(new GymWorkHour(day, LocalTime.of(7, 0), LocalTime.of(23, 0)));
            }
            gym2.setWorkHours(workHours2);

            if (fitnessCategory != null) {
                gym2.setCategories(new HashSet<>(Collections.singletonList(fitnessCategory)));
            }

            gym2 = gymRepository.save(gym2);

            String gymId2 = gym2.getGymId().toString();
            createTranslationIfNotFound("Gym", gymId2, "AZ", "name", "FitLife Studiyası");
            createTranslationIfNotFound("Gym", gymId2, "RU", "name", "Студия FitLife");
            createTranslationIfNotFound("Gym", gymId2, "AZ", "description", "Məqsədyönlü məşqlər üçün butik fitnes studiyası");
            createTranslationIfNotFound("Gym", gymId2, "RU", "description", "Бутик-студия фитнеса для целенаправленных тренировок");
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

            // Second Store
            StoreAddress address2 = new StoreAddress("789 Fit St", "Baku", 40.3955, 49.8505);

            Store store2 = new Store();
            store2.setName("Nutrition Plus");
            store2.setCategory("Supplements");
            store2.setStatus(StoreStatus.ACTIVE.name());
            store2.setCoverImageUrl("https://picsum.photos/seed/store2/800/600");
            store2.setLogoUrl("https://picsum.photos/seed/storelogo2/200/200");
            store2.setAddress(address2);
            store2.setPhone("+994510000001");
            store2.setPopularScore(8.5);

            store2 = storeRepository.save(store2);

            String storeId2 = store2.getStoreId().toString();
            createTranslationIfNotFound("Store", storeId2, "AZ", "name", "Qidalanma Plus");
            createTranslationIfNotFound("Store", storeId2, "RU", "name", "Питание Плюс");
            createTranslationIfNotFound("Store", storeId2, "AZ", "category", "Əlavələr");
            createTranslationIfNotFound("Store", storeId2, "RU", "category", "Добавки");
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

            // Second Trainer
            Trainer trainer2 = new Trainer();
            trainer2.setFirstName("Jane");
            trainer2.setLastName("Smith");
            trainer2.setSpecialization("Yoga");
            trainer2.setExperienceYears(8);
            trainer2.setRating(4.7);
            trainer2.setProfileImageUrl("https://i.pravatar.cc/150?u=trainer3");

            trainer2 = trainerRepository.save(trainer2);

            String trainerId2 = trainer2.getTrainerId().toString();
            createTranslationIfNotFound("Trainer", trainerId2, "AZ", "specialization", "Yoqa");
            createTranslationIfNotFound("Trainer", trainerId2, "RU", "specialization", "Йога");
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

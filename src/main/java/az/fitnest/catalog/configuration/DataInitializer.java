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
        createProfessionIfNotFound("CrossFit Coach");
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
            case "CrossFit Coach" -> "Krossfit Məşqçisi";
            default -> name;
        };
    }

    private String translateProfessionToRu(String name) {
        return switch (name) {
            case "Fitness Trainer" -> "Фитнес-тренер";
            case "Yoga Instructor" -> "Инструктор по йоге";
            case "Boxing Coach" -> "Тренер по боксу";
            case "Nutritionist" -> "Нутрициолог";
            case "CrossFit Coach" -> "Тренер по кроссфиту";
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
            gym.setQrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=gym1");
            gym.setAddress(address);
            gym.setPhone("+994500000000");
            gym.setEmail("info@premiumfitness.az");
            gym.setStatus(GymStatus.ACTIVE);
            gym.setRating(3.8461538461538463);
            gym.setReviewsCount(13);
            gym.setIsNew(true);

            List<GymWorkHour> workHours = new ArrayList<>();
            workHours.add(new GymWorkHour(DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(23, 0)));
            workHours.add(new GymWorkHour(DayOfWeek.TUESDAY, LocalTime.of(7, 0), LocalTime.of(23, 0)));
            workHours.add(new GymWorkHour(DayOfWeek.WEDNESDAY, LocalTime.of(7, 0), LocalTime.of(23, 0)));
            workHours.add(new GymWorkHour(DayOfWeek.THURSDAY, LocalTime.of(7, 0), LocalTime.of(23, 0)));
            workHours.add(new GymWorkHour(DayOfWeek.FRIDAY, LocalTime.of(7, 0), LocalTime.of(23, 0)));
            workHours.add(new GymWorkHour(DayOfWeek.SATURDAY, LocalTime.of(9, 0), LocalTime.of(21, 0)));
            workHours.add(new GymWorkHour(DayOfWeek.SUNDAY, LocalTime.of(10, 0), LocalTime.of(18, 0)));
            gym.setWorkHours(workHours);

            List<GymSocialLink> socialLinks = new ArrayList<>();
            socialLinks.add(new GymSocialLink("Instagram", "https://instagram.com/premiumfitness"));
            socialLinks.add(new GymSocialLink("Facebook", "https://facebook.com/premiumfitness"));
            gym.setSocialLinks(socialLinks);

            List<GymImage> images = new ArrayList<>();
            images.add(GymImage.builder().gym(gym).imageName("Main Hall").url("https://picsum.photos/seed/gym1/1200/800").type("interior").title("Main Hall - Main View").build());
            images.add(GymImage.builder().gym(gym).imageName("Main Hall").url("https://picsum.photos/seed/gym2/1200/800").type("interior").title("Main Hall - Equipment").build());
            images.add(GymImage.builder().gym(gym).imageName("Cardio Zone").url("https://picsum.photos/seed/gym3/1200/800").type("interior").title("Cardio Room").build());
            images.add(GymImage.builder().gym(gym).imageName("Swimming Pool").url("https://picsum.photos/seed/pool1/1200/800").type("interior").title("Olympic Pool").build());
            gym.setImages(images);

            if (fitnessCategory != null) {
                Category yogaCategory = categoryRepository.findAll().stream().filter(c -> c.getName().equals("Yoga")).findFirst().orElse(null);
                Set<Category> categories = new HashSet<>();
                categories.add(fitnessCategory);
                if (yogaCategory != null) categories.add(yogaCategory);
                gym.setCategories(categories);
            }

            gym = gymRepository.save(gym);

            Profession fitnessProfession = professionRepository.findAll().stream().filter(p -> p.getName().equals("Fitness Trainer")).findFirst().orElse(null);
            Profession yogaProfession = professionRepository.findAll().stream().filter(p -> p.getName().equals("Yoga Instructor")).findFirst().orElse(null);
            Profession crossfitProfession = professionRepository.findAll().stream().filter(p -> p.getName().equals("CrossFit Coach")).findFirst().orElse(null);

            Trainer trainer1 = new Trainer();
            trainer1.setFirstName("Eli");
            trainer1.setLastName("Memmedov");
            trainer1.setSpecialization("Fitness");
            trainer1.setExperienceYears(5);
            trainer1.setRating(4.8);
            trainer1.setProfileImageUrl("https://i.pravatar.cc/150?u=trainer1");
            trainer1.setPhone("+994501234567");
            trainer1.setEmail("eli@premiumfitness.az");
            trainer1.setProfession(fitnessProfession);

            Trainer trainer2 = new Trainer();
            trainer2.setFirstName("Lale");
            trainer2.setLastName("Resulova");
            trainer2.setSpecialization("Yoga");
            trainer2.setExperienceYears(3);
            trainer2.setRating(4.9);
            trainer2.setProfileImageUrl("https://i.pravatar.cc/150?u=trainer2");
            trainer2.setPhone("+994501234568");
            trainer2.setEmail("lale@premiumfitness.az");
            trainer2.setProfession(yogaProfession);

            Trainer trainer3 = new Trainer();
            trainer3.setFirstName("Tural");
            trainer3.setLastName("Aliyev");
            trainer3.setSpecialization("Crossfit");
            trainer3.setExperienceYears(7);
            trainer3.setRating(5.0);
            trainer3.setProfileImageUrl("https://i.pravatar.cc/150?u=trainer3");
            trainer3.setPhone("+994501234569");
            trainer3.setEmail("tural@premiumfitness.az");
            trainer3.setProfession(crossfitProfession);

            trainer1.setGymId(gym.getId());
            trainer2.setGymId(gym.getId());
            trainer3.setGymId(gym.getId());

            gym.setTrainers(new ArrayList<>(List.of(trainer1, trainer2, trainer3)));

            String[] commonReviewTexts = {
                    "Amazing gym with great trainers!",
                    "Very clean and modern equipment.",
                    "The pool is excellent.",
                    "Best fitness center in Baku!",
                    "Highly recommended for athletes.",
                    "Professional atmosphere.",
                    "Good value for membership.",
                    "Friendly staff and great community.",
                    "Love the yoga classes here.",
                    "Modern and spacious.",
                    "Great location and parking.",
                    "High quality services.",
                    "Excellent crossfit zone."
            };
            for (int i = 0; i < 13; i++) {
                Review review = new Review((long)(i + 1), (i % 2 == 0 ? 4 : 5), commonReviewTexts[i]);
                review.setGymId(gym.getId());
                gym.getReviews().add(review);
            }
            gym = gymRepository.save(gym);

            GymSubscription sub1_1 = new GymSubscription();
            sub1_1.setGym(gym);
            sub1_1.setPlanId(1L);
            sub1_1.setBenefits(new java.util.HashSet<>(Arrays.asList(
                    new GymSubscriptionBenefit("Access to 5 gyms", "https://img.icons8.com/color/96/medal-bronze.png"),
                    new GymSubscriptionBenefit("Email support", "https://img.icons8.com/color/96/customer-support.png")
            )));

            GymSubscription sub1_2 = new GymSubscription();
            sub1_2.setGym(gym);
            sub1_2.setPlanId(2L);
            sub1_2.setBenefits(new java.util.HashSet<>(Arrays.asList(
                    new GymSubscriptionBenefit("Access to 15 gyms", "https://img.icons8.com/color/96/medal-silver.png"),
                    new GymSubscriptionBenefit("Priority email support", "https://img.icons8.com/color/96/customer-support.png"),
                    new GymSubscriptionBenefit("Sauna access", "https://img.icons8.com/color/96/sauna.png")
            )));

            GymSubscription sub1_3 = new GymSubscription();
            sub1_3.setGym(gym);
            sub1_3.setPlanId(3L);
            sub1_3.setBenefits(new java.util.HashSet<>(Arrays.asList(
                    new GymSubscriptionBenefit("Access to all gyms", "https://img.icons8.com/color/96/medal-gold.png"),
                    new GymSubscriptionBenefit("24/7 support", "https://img.icons8.com/color/96/customer-support.png"),
                    new GymSubscriptionBenefit("Pool access", "https://img.icons8.com/color/96/swimming-pool.png"),
                    new GymSubscriptionBenefit("1 monthly personal trainer session", "https://img.icons8.com/color/96/personal-trainer.png")
            )));

            GymSubscription sub1_4 = new GymSubscription();
            sub1_4.setGym(gym);
            sub1_4.setPlanId(4L);
            sub1_4.setBenefits(new java.util.HashSet<>(Arrays.asList(
                    new GymSubscriptionBenefit("Unlimited access to all gyms", "https://img.icons8.com/color/96/diamond.png"),
                    new GymSubscriptionBenefit("Dedicated account manager", "https://img.icons8.com/color/96/conference-call.png"),
                    new GymSubscriptionBenefit("VIP lounge access", "https://img.icons8.com/color/96/vip.png"),
                    new GymSubscriptionBenefit("Weekly personal trainer sessions", "https://img.icons8.com/color/96/personal-trainer.png")
            )));

            gym.setSubscriptions(new HashSet<>(Arrays.asList(sub1_1, sub1_2, sub1_3, sub1_4)));
            gymRepository.save(gym);

            String gymId = gym.getGymId().toString();
            createTranslationIfNotFound("Gym", gymId, "AZ", "name", "Premium Fitnes Mərkəzi");
            createTranslationIfNotFound("Gym", gymId, "RU", "name", "Премиум Фитнес Центр");
            createTranslationIfNotFound("Gym", gymId, "AZ", "description", "Müasir avadanlıq və peşəkar məşqçilər ilə yüksək keyfiyyətli fitnes mərkəzi");
            createTranslationIfNotFound("Gym", gymId, "RU", "description", "Высококачественный фитнес-центр с современным оборудованием и профессиональными тренерами");

            Address address2 = Address.builder()
                    .addressText("456 Secondary St")
                    .city("Baku")
                    .latitude(40.3850)
                    .longitude(49.8250)
                    .build();

            Gym gym2 = new Gym();
            gym2.setName("Peak Performance");
            gym2.setDescription("The ultimate training ground for athletes and fitness enthusiasts.");
            gym2.setCoverImageUrl("https://picsum.photos/seed/gym2/800/600");
            gym2.setAddress(address2);
            gym2.setPhone("+994500000001");
            gym2.setEmail("info@peakperformance.az");
            gym2.setStatus(GymStatus.ACTIVE);
            gym2.setRating(4.9);
            gym2.setReviewsCount(45);
            gym2.setIsNew(true);

            List<GymWorkHour> workHours2 = new ArrayList<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                workHours2.add(new GymWorkHour(day, LocalTime.of(7, 0), LocalTime.of(23, 0)));
            }
            gym2.setWorkHours(workHours2);

            List<GymImage> images2 = new ArrayList<>();
            images2.add(GymImage.builder()
                    .gym(gym2)
                    .imageName("Crossfit Area")
                    .url("https://picsum.photos/seed/gym2/1200/800")
                    .type("interior")
                    .title("Crossfit & Functional Zone")
                    .build());
            gym2.setImages(images2);

            if (fitnessCategory != null) {
                gym2.setCategories(new HashSet<>(Collections.singletonList(fitnessCategory)));
            }

            gym2 = gymRepository.save(gym2);

            Trainer trainer4 = new Trainer();
            trainer4.setFirstName("Tural");
            trainer4.setLastName("Aliyev");
            trainer4.setSpecialization("Crossfit");
            trainer4.setExperienceYears(7);
            trainer4.setRating(5.0);
            trainer4.setProfileImageUrl("https://i.pravatar.cc/150?u=trainer3");
            trainer4.setPhone("+994501234569");
            trainer4.setEmail("tural@peakperformance.az");
            Profession profession3 = professionRepository.findAll().stream().filter(p -> p.getName().equals("CrossFit Coach")).findFirst().orElse(null);
            trainer4.setProfession(profession3);

            gym2.getTrainers().add(trainer4);

            GymSubscription sub2_1 = new GymSubscription();
            sub2_1.setGym(gym2);
            sub2_1.setPlanId(1L);
            sub2_1.setBenefits(new java.util.HashSet<>(Arrays.asList(
                    new GymSubscriptionBenefit("Access to 5 gyms", "https://img.icons8.com/color/96/medal-bronze.png"),
                    new GymSubscriptionBenefit("Basic training support", "https://img.icons8.com/color/96/customer-support.png")
            )));

            GymSubscription sub2_2 = new GymSubscription();
            sub2_2.setGym(gym2);
            sub2_2.setPlanId(2L);
            sub2_2.setBenefits(new java.util.HashSet<>(Arrays.asList(
                    new GymSubscriptionBenefit("Access to 15 gyms", "https://img.icons8.com/color/96/medal-silver.png"),
                    new GymSubscriptionBenefit("Sauna & Steam room", "https://img.icons8.com/color/96/sauna.png")
            )));

            GymSubscription sub2_3 = new GymSubscription();
            sub2_3.setGym(gym2);
            sub2_3.setPlanId(3L);
            sub2_3.setBenefits(new java.util.HashSet<>(Arrays.asList(
                    new GymSubscriptionBenefit("Access to all gyms", "https://img.icons8.com/color/96/medal-gold.png"),
                    new GymSubscriptionBenefit("Pool access", "https://img.icons8.com/color/96/swimming-pool.png"),
                    new GymSubscriptionBenefit("Spa access", "https://img.icons8.com/color/96/spa.png")
            )));

            GymSubscription sub2_4 = new GymSubscription();
            sub2_4.setGym(gym2);
            sub2_4.setPlanId(4L);
            sub2_4.setBenefits(new java.util.HashSet<>(Arrays.asList(
                    new GymSubscriptionBenefit("Unlimited access to all gyms", "https://img.icons8.com/color/96/diamond.png"),
                    new GymSubscriptionBenefit("VIP lounge access", "https://img.icons8.com/color/96/vip.png"),
                    new GymSubscriptionBenefit("Personal trainer weekly", "https://img.icons8.com/color/96/personal-trainer.png")
            )));

            gym2.setSubscriptions(new HashSet<>(Arrays.asList(sub2_1, sub2_2, sub2_3, sub2_4)));
            gymRepository.save(gym2);

            String gymId2 = gym2.getGymId().toString();
            createTranslationIfNotFound("Gym", gymId2, "AZ", "name", "Peak Performance Mərkəzi");
            createTranslationIfNotFound("Gym", gymId2, "RU", "name", "Центр Peak Performance");
            createTranslationIfNotFound("Gym", gymId2, "AZ", "description", "İdmançılar və fitnes həvəskarları üçün son məşq meydançası.");
            createTranslationIfNotFound("Gym", gymId2, "RU", "description", "Идеальная тренировочная площадка для спортсменов и любителей фитнеса.");
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

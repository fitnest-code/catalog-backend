package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.Store;
import az.fitnest.catalog.model.entity.Translation;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.StoreRepository;
import az.fitnest.catalog.repository.TranslationRepository;
import az.fitnest.catalog.service.AddressMigrationService;
import az.fitnest.catalog.util.AddressParseUtil;
import az.fitnest.catalog.util.AzerbaijanLocations;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressMigrationServiceImpl implements AddressMigrationService {

    private final GymRepository gymRepository;
    private final StoreRepository storeRepository;
    private final TranslationRepository translationRepository;

    @Override
    @Transactional
    public MigrationResult migrateLegacyAddresses(boolean dryRun) {
        int gymsUpdated = 0;
        int storesUpdated = 0;
        int translationsUpdated = 0;

        List<Gym> gyms = gymRepository.findAll();
        for (Gym gym : gyms) {
            if (gym.getAddress() == null) continue;
            Address address = gym.getAddress();
            AddressParseUtil.ParsedAddress parsed = AddressParseUtil.parse(address.getCity(), address.getAddressText());
            if (!needsUpdate(address, parsed)) continue;
            gymsUpdated++;
            if (dryRun) {
                log.info("[dry-run] gym {} -> city={} rayon={} address={}",
                        gym.getId(), parsed.city(), parsed.rayon(), parsed.addressText());
                continue;
            }
            apply(address, parsed);
            gymRepository.save(gym);
            translationsUpdated += updateTranslations("GYM", gym.getId().toString(), parsed);
        }

        List<Store> stores = storeRepository.findAll();
        for (Store store : stores) {
            if (store.getAddress() == null) continue;
            Address address = store.getAddress();
            AddressParseUtil.ParsedAddress parsed = AddressParseUtil.parse(address.getCity(), address.getAddressText());
            if (!needsUpdate(address, parsed)) continue;
            storesUpdated++;
            if (dryRun) {
                log.info("[dry-run] store {} -> city={} rayon={} address={}",
                        store.getId(), parsed.city(), parsed.rayon(), parsed.addressText());
                continue;
            }
            apply(address, parsed);
            storeRepository.save(store);
            translationsUpdated += updateTranslations("STORE", store.getId().toString(), parsed);
        }

        return new MigrationResult(gymsUpdated, storesUpdated, translationsUpdated, dryRun);
    }

    private boolean needsUpdate(Address address, AddressParseUtil.ParsedAddress parsed) {
        boolean cityChanged = !Objects.equals(nullToEmpty(address.getCity()), nullToEmpty(parsed.city()));
        boolean rayonEmpty = address.getRayon() == null || address.getRayon().isBlank();
        boolean rayonChanged = !Objects.equals(nullToEmpty(address.getRayon()), nullToEmpty(parsed.rayon()));
        boolean streetChanged = parsed.addressText() != null
                && !Objects.equals(nullToEmpty(address.getAddressText()), nullToEmpty(parsed.addressText()));
        // Migrate when rayon is missing but parse found one, or street/city need cleanup
        return (rayonEmpty && parsed.rayon() != null) || cityChanged || (rayonChanged && parsed.rayon() != null) || streetChanged;
    }

    private void apply(Address address, AddressParseUtil.ParsedAddress parsed) {
        if (parsed.city() != null) {
            address.setCity(parsed.city());
        }
        address.setRayon(AzerbaijanLocations.isBaki(parsed.city()) ? parsed.rayon() : null);
        if (parsed.addressText() != null) {
            address.setAddressText(parsed.addressText());
        }
    }

    private int updateTranslations(String entityType, String entityId, AddressParseUtil.ParsedAddress parsed) {
        int updated = 0;
        updated += upsertTranslation(entityType, entityId, "city", parsed.city());
        updated += upsertTranslation(entityType, entityId, "rayon", parsed.rayon());
        updated += upsertTranslation(entityType, entityId, "addressText", parsed.addressText());
        return updated;
    }

    private int upsertTranslation(String entityType, String entityId, String field, String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        // Proper-noun fields are stored as AZ source for all languages
        for (String lang : List.of("AZ", "EN", "RU")) {
            Translation existing = translationRepository
                    .findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(entityType, entityId, lang, field)
                    .orElse(null);
            if (existing == null) {
                Translation t = new Translation();
                t.setEntityType(entityType);
                t.setEntityId(entityId);
                t.setFieldName(field);
                t.setLanguageCode(lang);
                t.setFieldValue(value);
                translationRepository.save(t);
            } else if (!value.equals(existing.getFieldValue())) {
                existing.setFieldValue(value);
                translationRepository.save(existing);
            }
        }
        return 1;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

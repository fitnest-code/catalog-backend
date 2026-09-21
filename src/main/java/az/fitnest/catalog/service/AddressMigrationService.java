package az.fitnest.catalog.service;

/**
 * One-shot migration that splits legacy addressText into city / rayon / street
 * for gyms and stores (and matching translation rows).
 */
public interface AddressMigrationService {
    MigrationResult migrateLegacyAddresses(boolean dryRun);

    record MigrationResult(int gymsUpdated, int storesUpdated, int translationsUpdated, boolean dryRun) {
    }
}

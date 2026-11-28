package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.request.CreateTranslationRequest;
import az.fitnest.catalog.model.entity.Translation;
import az.fitnest.catalog.repository.TranslationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranslationServiceBulkTest {

    @Mock
    private TranslationRepository translationRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    private TranslationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TranslationServiceImpl(translationRepository, cacheManager);
    }

    @Test
    void upsertsNewAndExistingValuesInOneBatchAndClearsCaches() {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        Translation existing = Translation.builder()
                .entityType("GYM").entityId("42").languageCode("EN")
                .fieldName("description").fieldValue("old").build();
        when(translationRepository.findByEntityTypeAndEntityIdIn("GYM", java.util.Set.of("42")))
                .thenReturn(List.of(existing));

        service.saveOrUpdateTranslations(List.of(
                new CreateTranslationRequest("gym", "42", "en", "description", "About EN"),
                new CreateTranslationRequest("GYM", "42", "RU", "description", "О нас")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Translation>> captor = ArgumentCaptor.forClass(List.class);
        verify(translationRepository).saveAll(captor.capture());
        List<Translation> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals("About EN", saved.get(0).getFieldValue());
        assertEquals("RU", saved.get(1).getLanguageCode());
        assertEquals("О нас", saved.get(1).getFieldValue());
        verify(cache, atLeastOnce()).clear();
    }

    @Test
    void blankValueClearsExistingTranslationAndDoesNotCreateNewRows() {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        Translation existing = Translation.builder()
                .entityType("GYM").entityId("42").languageCode("EN")
                .fieldName("description").fieldValue("old").build();
        when(translationRepository.findByEntityTypeAndEntityIdIn("GYM", java.util.Set.of("42")))
                .thenReturn(List.of(existing));

        service.saveOrUpdateTranslations(List.of(
                new CreateTranslationRequest("GYM", "42", "EN", "description", "   "),
                new CreateTranslationRequest("GYM", "42", "RU", "description", null)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Translation>> captor = ArgumentCaptor.forClass(List.class);
        verify(translationRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertNull(captor.getValue().get(0).getFieldValue());
    }

    @Test
    void sharedGymNameIsNeverTranslated() {
        service.saveOrUpdateTranslations(List.of(
                new CreateTranslationRequest("GYM", "42", "EN", "name", "Another name")));

        verify(translationRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void azReturnsNullAndEnReturnsStoredValue() {
        when(translationRepository.findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                "GYM", "42", "EN", "description"))
                .thenReturn(Optional.of(Translation.builder()
                        .entityType("GYM").entityId("42").languageCode("EN")
                        .fieldName("description").fieldValue("About EN").build()));

        assertNull(service.getTranslatedValue("GYM", "42", "description", "AZ"));
        assertEquals("About EN", service.getTranslatedValue("GYM", "42", "description", "EN"));
        verify(translationRepository, never()).findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                "GYM", "42", "AZ", "description");

        String missing = service.getTranslatedValue("GYM", "42", "description", "RU");
        assertTrue(missing == null || missing.isEmpty());
    }

    @Test
    void nullAndEmptyBatchesDoNothing() {
        service.saveOrUpdateTranslations(null);
        service.saveOrUpdateTranslations(List.of());

        verifyNoInteractions(translationRepository, cacheManager);
    }

    @Test
    void loadsEachEntityTypeOnceAndSkipsIncompleteRequests() {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(translationRepository.findByEntityTypeAndEntityIdIn("GYM", Set.of("42"))).thenReturn(List.of());
        when(translationRepository.findByEntityTypeAndEntityIdIn("SUPPORTEDSERVICE", Set.of("11")))
                .thenReturn(List.of());

        service.saveOrUpdateTranslations(java.util.Arrays.asList(
                null,
                new CreateTranslationRequest("GYM", "42", "EN", "description", "About EN"),
                new CreateTranslationRequest("GYM", null, "RU", "description", "ignored"),
                new CreateTranslationRequest("SUPPORTEDSERVICE", "11", "ru", "name", "Сауна")));

        verify(translationRepository).findByEntityTypeAndEntityIdIn("GYM", Set.of("42"));
        verify(translationRepository).findByEntityTypeAndEntityIdIn("SUPPORTEDSERVICE", Set.of("11"));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Translation>> captor = ArgumentCaptor.forClass(List.class);
        verify(translationRepository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    void duplicateKeysProduceASingleUpsert() {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(translationRepository.findByEntityTypeAndEntityIdIn("GYM", Set.of("42"))).thenReturn(List.of());

        service.saveOrUpdateTranslations(List.of(
                new CreateTranslationRequest("GYM", "42", "en", "description", "first"),
                new CreateTranslationRequest("gym", "42", "EN", "description", "second")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Translation>> captor = ArgumentCaptor.forClass(List.class);
        verify(translationRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        // Duplicate keys are collapsed before the upsert: the first value in the payload wins.
        assertEquals("first", captor.getValue().get(0).getFieldValue());
    }

    @Test
    void existingRowWithLowercaseLanguageIsUpdatedNotDuplicated() {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        Translation existing = Translation.builder()
                .entityType("GYM").entityId("42").languageCode("en")
                .fieldName("description").fieldValue("old").build();
        when(translationRepository.findByEntityTypeAndEntityIdIn("GYM", Set.of("42")))
                .thenReturn(List.of(existing));

        service.saveOrUpdateTranslations(List.of(
                new CreateTranslationRequest("gym", "42", "EN", "description", "new")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Translation>> captor = ArgumentCaptor.forClass(List.class);
        verify(translationRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("new", captor.getValue().get(0).getFieldValue());
    }

    @Test
    void copyAzForProperNounsStillSeedsAddressesButNotDescriptions() {
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        when(translationRepository.findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());

        service.copyAzForProperNouns("GYM", "42", "description", "About AZ");
        verify(translationRepository, never()).save(org.mockito.ArgumentMatchers.any(Translation.class));

        service.copyAzForProperNouns("GYM", "42", "addressText", "Street 1");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Translation> captor = ArgumentCaptor.forClass(Translation.class);
        verify(translationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(Set.of("EN", "RU"), captor.getAllValues().stream()
                .map(Translation::getLanguageCode).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void sharedGymNameTranslationIsStillIgnoredOnRead() {
        assertNull(service.getTranslatedValue("GYM", "42", "name", "EN"));
        assertEquals("Активный", service.getTranslatedValue("GYM_STATUS", "ACTIVE", "status", "RU"));
        verifyNoInteractions(translationRepository);
    }
}

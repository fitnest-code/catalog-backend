package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.request.CategoryDetailV3;
import az.fitnest.catalog.dto.request.CreateTranslationRequest;
import az.fitnest.catalog.dto.request.LocalizedServiceNameRequest;
import az.fitnest.catalog.model.entity.SupportedService;
import az.fitnest.catalog.repository.SupportedServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GymTranslationWriterTest {

    @Mock
    private TranslationService translationService;
    @Mock
    private SupportedServiceRepository supportedServiceRepository;
    @InjectMocks
    private GymTranslationWriter writer;

    @Test
    void savesGymAndEveryCategoryDescriptionInEnAndRu() {
        writer.saveDescriptions(42L, "About EN", "О нас", List.of(
                new CategoryDetailV3(1L, null, "AZ main", "Main EN", "Main RU", null),
                new CategoryDetailV3(2L, null, "AZ sub", "Sub EN", "Sub RU", null)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CreateTranslationRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(translationService).saveOrUpdateTranslations(captor.capture());
        List<CreateTranslationRequest> requests = captor.getValue();

        assertEquals(6, requests.size());
        assertTrue(requests.contains(new CreateTranslationRequest("GYM", "42", "EN", "description", "About EN")));
        assertTrue(requests.contains(new CreateTranslationRequest("GYM", "42", "RU", "description", "О нас")));
        assertTrue(requests.contains(new CreateTranslationRequest("GYM", "42", "EN", "description_1", "Main EN")));
        assertTrue(requests.contains(new CreateTranslationRequest("GYM", "42", "RU", "description_2", "Sub RU")));
        assertTrue(requests.stream().noneMatch(r -> r.languageCode().equalsIgnoreCase("AZ")));
        assertTrue(requests.stream().noneMatch(r -> r.fieldName().equals("name")));
    }

    @Test
    void blankLocalizedValuesAreNotStoredSoAzFallbackApplies() {
        writer.saveDescriptions(7L, "  ", null,
                List.of(new CategoryDetailV3(3L, null, "AZ", "", null, null)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CreateTranslationRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(translationService).saveOrUpdateTranslations(captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    void savesCustomServiceNamesOnlyForGymSpecificServices() {
        SupportedService custom = SupportedService.builder().gymId(42L).name("  Massage  ").build();
        custom.setId(11L);
        when(supportedServiceRepository.findAllByGymId(42L)).thenReturn(List.of(custom));

        writer.saveCustomServiceNames(42L, List.of(
                new LocalizedServiceNameRequest("Massage", "Massage EN", "Массаж"),
                new LocalizedServiceNameRequest("Racket", "Racket EN", "Ракетка"),
                new LocalizedServiceNameRequest("  ", "ignored", "ignored")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CreateTranslationRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(translationService).saveOrUpdateTranslations(captor.capture());
        List<CreateTranslationRequest> requests = captor.getValue();

        assertEquals(2, requests.size());
        assertTrue(requests.contains(
                new CreateTranslationRequest("SUPPORTEDSERVICE", "11", "EN", "name", "Massage EN")));
        assertTrue(requests.contains(
                new CreateTranslationRequest("SUPPORTEDSERVICE", "11", "RU", "name", "Массаж")));
    }

    @Test
    void skipsNullEntriesAndStillSavesValidCategoryTranslations() {
        writer.saveDescriptions(7L, null, null, Arrays.asList(
                null,
                new CategoryDetailV3(null, null, "AZ", "ignored", "ignored", null),
                new CategoryDetailV3(3L, null, null, "EN 3", null, null)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CreateTranslationRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(translationService).saveOrUpdateTranslations(captor.capture());
        assertEquals(List.of(new CreateTranslationRequest("GYM", "7", "EN", "description_3", "EN 3")),
                captor.getValue());
    }

    @Test
    void nullCustomServicesDoNotTouchRepositories() {
        writer.saveCustomServiceNames(7L, null);

        verifyNoInteractions(translationService, supportedServiceRepository);
    }

    @Test
    void duplicateServiceNamesAreWrittenOnlyOnceAndNullNamedServicesAreIgnored() {
        SupportedService sauna = SupportedService.builder().gymId(7L).name("Sauna").build();
        sauna.setId(12L);
        SupportedService broken = SupportedService.builder().gymId(7L).name(null).build();
        broken.setId(13L);
        when(supportedServiceRepository.findAllByGymId(7L)).thenReturn(List.of(sauna, broken));

        writer.saveCustomServiceNames(7L, List.of(
                new LocalizedServiceNameRequest("Sauna", "Sauna EN", "Сауна"),
                new LocalizedServiceNameRequest(" sauna ", "Sauna EN", "Сауна")));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CreateTranslationRequest>> captor = ArgumentCaptor.forClass(List.class);
        verify(translationService).saveOrUpdateTranslations(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertTrue(captor.getValue().contains(
                new CreateTranslationRequest("SUPPORTEDSERVICE", "12", "EN", "name", "Sauna EN")));
        assertTrue(captor.getValue().contains(
                new CreateTranslationRequest("SUPPORTEDSERVICE", "12", "RU", "name", "Сауна")));
    }
}

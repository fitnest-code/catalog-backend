package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.client.UserServiceGrpcClient;
import az.fitnest.catalog.dto.ProfessionDto;
import az.fitnest.catalog.dto.ProfessionRequest;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Profession;
import az.fitnest.catalog.repository.ProfessionRepository;
import az.fitnest.catalog.repository.TrainerRepository;
import az.fitnest.catalog.service.TranslationService;
import az.fitnest.catalog.util.UserContext;
import az.fitnest.user.grpc.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionServiceImpl implements az.fitnest.catalog.service.ProfessionService {

    private final ProfessionRepository professionRepository;
    private final TrainerRepository trainerRepository;
    private final TranslationService translationService;
    private final UserServiceGrpcClient userServiceGrpcClient;

    @Transactional(readOnly = true)
    public List<ProfessionDto> getAllProfessions() {
        String language = resolveUserLanguage();
        return professionRepository.findAll().stream()
                .map(p -> toDto(p, language))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProfessionDto getProfessionById(Long id) {
        Profession p = professionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PROFESSION_NOT_FOUND", "error.profession_not_found"));
        return toDto(p, resolveUserLanguage());
    }

    @Transactional
    public ProfessionDto createProfession(ProfessionRequest request) {
        if (professionRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("error.profession_already_exists");
        }
        Profession p = new Profession();
        p.setName(request.name());
        Profession saved = professionRepository.save(p);
        return toDto(saved, resolveUserLanguage());
    }

    @Transactional
    public ProfessionDto updateProfession(Long id, ProfessionRequest request) {
        Profession p = professionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PROFESSION_NOT_FOUND", "error.profession_not_found"));

        if (!p.getName().equals(request.name()) && professionRepository.existsByName(request.name())) {
            throw new az.fitnest.catalog.exception.BadRequestException("PROFESSION_ALREADY_EXISTS", "error.profession_already_exists");
        }

        p.setName(request.name());
        Profession saved = professionRepository.save(p);
        return toDto(saved, resolveUserLanguage());
    }

    @Transactional
    public void deleteProfession(Long id) {
        Profession p = professionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PROFESSION_NOT_FOUND", "Peşə tapılmadı"));
        professionRepository.delete(p);
    }

    @Transactional
    public void deleteAllProfessions() {
        trainerRepository.clearAllProfessions();
        professionRepository.deleteAll();
    }

    private ProfessionDto toDto(Profession profession, String language) {
        String localizedName = translationService.getTranslatedValue("PROFESSION", String.valueOf(profession.getId()), "name", language);
        if (localizedName == null || localizedName.isEmpty()) {
            localizedName = profession.getName();
        }
        return ProfessionDto.builder()
                .id(profession.getId())
                .name(localizedName)
                .build();
    }

    private String resolveUserLanguage() {
        Long userId = UserContext.getCurrentUserId();
        return resolveUserLanguage(userId);
    }

    private String resolveUserLanguage(Long userId) {
        if (userId != null) {
            try {
                UserResponse user = userServiceGrpcClient.getUserById(userId);
                if (user != null && user.getLanguage() != null && !user.getLanguage().isEmpty()) {
                    return user.getLanguage();
                }
            } catch (Exception ignored) {
            }
        }
        return "AZ";
    }
}

package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.ProfessionDto;
import az.fitnest.catalog.dto.ProfessionRequest;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Profession;
import az.fitnest.catalog.repository.ProfessionRepository;
import az.fitnest.catalog.repository.TrainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionService {

    private final ProfessionRepository professionRepository;
    private final TrainerRepository trainerRepository;

    @Transactional(readOnly = true)
    public List<ProfessionDto> getAllProfessions() {
        return professionRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProfessionDto getProfessionById(Long id) {
        Profession p = professionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PROFESSION_NOT_FOUND", "error.profession_not_found"));
        return toDto(p);
    }

    @Transactional
    public ProfessionDto createProfession(ProfessionRequest request) {
        if (professionRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("error.profession_already_exists");
        }
        Profession p = new Profession();
        p.setName(request.name());
        return toDto(professionRepository.save(p));
    }

    @Transactional
    public ProfessionDto updateProfession(Long id, ProfessionRequest request) {
        Profession p = professionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PROFESSION_NOT_FOUND", "error.profession_not_found"));

        if (!p.getName().equals(request.name()) && professionRepository.existsByName(request.name())) {
            throw new az.fitnest.catalog.exception.BadRequestException("PROFESSION_ALREADY_EXISTS", "error.profession_already_exists");
        }

        p.setName(request.name());
        return toDto(professionRepository.save(p));
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

    private ProfessionDto toDto(Profession profession) {
        return ProfessionDto.builder()
                .id(profession.getId())
                .name(profession.getName())
                .build();
    }
}

package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.ProfessionDto;
import az.fitnest.catalog.dto.ProfessionRequest;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Profession;
import az.fitnest.catalog.repository.ProfessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionService {

    private final ProfessionRepository professionRepository;

    @Transactional(readOnly = true)
    public List<ProfessionDto> getAllProfessions() {
        return professionRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProfessionDto getProfessionById(Long id) {
        Profession p = professionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PROFESSION_NOT_FOUND", "Profession not found"));
        return toDto(p);
    }

    @Transactional
    public ProfessionDto createProfession(ProfessionRequest request) {
        if (professionRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Profession with this name already exists");
        }
        Profession p = Profession.builder()
                .name(request.getName())
                .build();
        return toDto(professionRepository.save(p));
    }

    @Transactional
    public ProfessionDto updateProfession(Long id, ProfessionRequest request) {
        Profession p = professionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PROFESSION_NOT_FOUND", "Profession not found"));
                
        if (!p.getName().equals(request.getName()) && professionRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Profession with this name already exists");
        }
        
        p.setName(request.getName());
        return toDto(professionRepository.save(p));
    }

    @Transactional
    public void deleteProfession(Long id) {
        Profession p = professionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PROFESSION_NOT_FOUND", "Profession not found"));
        professionRepository.delete(p);
    }

    @Transactional
    public void deleteAllProfessions() {
        professionRepository.deleteAll();
    }

    private ProfessionDto toDto(Profession profession) {
        return ProfessionDto.builder()
                .id(profession.getId())
                .name(profession.getName())
                .build();
    }
}

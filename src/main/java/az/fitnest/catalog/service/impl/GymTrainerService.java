package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.GymTrainerDto;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.TrainerRequest;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Profession;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.repository.ProfessionRepository;
import az.fitnest.catalog.repository.TrainerRepository;
import az.fitnest.catalog.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GymTrainerService {

    private final GymRepository gymRepository;
    private final TrainerRepository trainerRepository;
    private final ProfessionRepository professionRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public PaginatedResponse<GymTrainerDto> getTrainers(Long gymId, int page, int pageSize, String sortDir) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found");
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<Trainer> trainerPage = trainerRepository.findByGymId(gymId, pageable(page, pageSize, Sort.by(direction, "id")));
        List<GymTrainerDto> items = trainerPage.getContent().stream()
                .map(this::toGymTrainerDto)
                .collect(Collectors.toList());

        return PaginatedResponse.<GymTrainerDto>builder()
                .items(items)
                .total(trainerPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    @Transactional
    @CacheEvict(cacheNames = "gyms", key = "#gymId")
    public void addTrainer(Long gymId, TrainerRequest request) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found");
        }
        Trainer trainer = new Trainer();
        trainer.setGymId(gymId);
        updateTrainerFromRequest(trainer, request);
        trainerRepository.save(trainer);
    }

    @Transactional
    @CacheEvict(cacheNames = "gyms", key = "#gymId")
    public void updateTrainer(Long gymId, Long trainerId, TrainerRequest request) {
        Trainer trainer = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));
        if (!gymId.equals(trainer.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "Məşqçi tapılmadı");
        }

        if (request.picture() != null && !request.picture().equals(trainer.getPicture())) {
            safeDeleteFile(trainer.getPicture());
        }

        updateTrainerFromRequest(trainer, request);
        trainerRepository.save(trainer);
    }

    @Transactional
    @CacheEvict(cacheNames = "gyms", key = "#gymId")
    public void deleteTrainer(Long gymId, Long trainerId) {
        Trainer trainerToDelete = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found"));
        if (!gymId.equals(trainerToDelete.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "error.trainer_not_found");
        }

        if (trainerToDelete.getPicture() != null && !trainerToDelete.getPicture().isBlank()) {
            safeDeleteFile(trainerToDelete.getPicture());
        }
        trainerRepository.delete(trainerToDelete);
    }

    private void updateTrainerFromRequest(Trainer trainer, TrainerRequest request) {
        trainer.setName(request.name());
        trainer.setSurname(request.surname());

        Profession profession = professionRepository.findById(request.professionId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFESSION_NOT_FOUND", "Peşə tapılmadı"));
        trainer.setProfession(profession);

        trainer.setPicture(request.picture());
        trainer.setPhone(request.phone());
        trainer.setEmail(request.email());
    }

    private void safeDeleteFile(String url) {
        try {
            fileStorageService.deleteFile(url);
        } catch (Exception e) {
            // Background error on deletion - swallowed on purpose
        }
    }

    private GymTrainerDto toGymTrainerDto(Trainer t) {
        az.fitnest.catalog.dto.ProfessionDto professionDto = null;
        if (t.getProfession() != null) {
            professionDto = az.fitnest.catalog.dto.ProfessionDto.builder()
                    .id(t.getProfession().getId())
                    .name(t.getProfession().getName())
                    .build();
        }

        return GymTrainerDto.builder()
                .trainer_id(t.getId() != null ? t.getId().toString() : null)
                .name(t.getName())
                .surname(t.getSurname())
                .profession(professionDto)
                .picture(t.getPicture())
                .phone(t.getPhone())
                .email(t.getEmail())
                .build();
    }

    private Pageable pageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 1) - 1;
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, sort);
    }
}

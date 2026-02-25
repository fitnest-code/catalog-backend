package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.GymTrainerDto;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.TrainerRequest;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.repository.GymRepository;
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
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public PaginatedResponse<GymTrainerDto> getTrainers(Long gymId, int page, int pageSize) {
        if (!gymRepository.existsById(gymId)) {
            throw new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found");
        }
        Page<Trainer> trainerPage = trainerRepository.findByGymId(gymId, pageable(page, pageSize, Sort.unsorted()));
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
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "Trainer not found"));
        if (!gymId.equals(trainer.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "Trainer not found");
        }
        
        if (request.getImageUrl() != null && !request.getImageUrl().equals(trainer.getImageUrl())) {
            safeDeleteFile(trainer.getImageUrl());
        }
        
        updateTrainerFromRequest(trainer, request);
        trainerRepository.save(trainer);
    }

    @Transactional
    @CacheEvict(cacheNames = "gyms", key = "#gymId")
    public void deleteTrainer(Long gymId, Long trainerId) {
        Trainer trainerToDelete = trainerRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("TRAINER_NOT_FOUND", "Trainer not found"));
        if (!gymId.equals(trainerToDelete.getGymId())) {
            throw new ResourceNotFoundException("TRAINER_NOT_FOUND", "Trainer not found");
        }
        
        if (trainerToDelete.getImageUrl() != null && !trainerToDelete.getImageUrl().isBlank()) {
            safeDeleteFile(trainerToDelete.getImageUrl());
        }
        trainerRepository.delete(trainerToDelete);
    }

    private void updateTrainerFromRequest(Trainer trainer, TrainerRequest request) {
        trainer.setFullName(request.getFullName());
        trainer.setSpecialization(request.getSpecialization());
        trainer.setImageUrl(request.getImageUrl());
    }

    private void safeDeleteFile(String url) {
        try {
            fileStorageService.deleteFile(url);
        } catch (Exception e) {
            // Background error on deletion - swallowed on purpose
        }
    }

    private GymTrainerDto toGymTrainerDto(Trainer t) {
        return GymTrainerDto.builder()
                .trainer_id(t.getId() != null ? t.getId().toString() : null)
                .full_name(t.getFullName())
                .specialization(t.getSpecialization())
                .image_url(t.getImageUrl())
                .build();
    }

    private Pageable pageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 1) - 1;
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, sort);
    }
}

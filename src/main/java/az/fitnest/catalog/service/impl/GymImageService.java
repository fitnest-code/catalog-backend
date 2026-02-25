package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.GymImageDto;
import az.fitnest.catalog.exception.BadRequestException;
import az.fitnest.catalog.exception.ResourceNotFoundException;
import az.fitnest.catalog.model.entity.Gym;
import az.fitnest.catalog.model.entity.GymImage;
import az.fitnest.catalog.repository.GymImageRepository;
import az.fitnest.catalog.repository.GymRepository;
import az.fitnest.catalog.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GymImageService {

    private final GymRepository gymRepository;
    private final GymImageRepository gymImageRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages"}, key = "#gymId")
    public void updateLogoUrl(Long gymId, String url) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        if (gym.getLogoUrl() != null && !gym.getLogoUrl().equals(url)) {
            safeDeleteFile(gym.getLogoUrl());
        }
        gym.setLogoUrl(url);
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages"}, key = "#gymId")
    public void deleteLogoUrl(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        if (gym.getLogoUrl() != null && !gym.getLogoUrl().isBlank()) {
            safeDeleteFile(gym.getLogoUrl());
            gym.setLogoUrl(null);
            gymRepository.save(gym);
        }
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages"}, key = "#gymId")
    public void updateCoverImageUrl(Long gymId, String url) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        if (gym.getCoverImageUrl() != null && !gym.getCoverImageUrl().equals(url)) {
            safeDeleteFile(gym.getCoverImageUrl());
        }
        gym.setCoverImageUrl(url);
        gymRepository.save(gym);
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages"}, key = "#gymId")
    public void deleteCoverImageUrl(Long gymId) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        if (gym.getCoverImageUrl() != null && !gym.getCoverImageUrl().isBlank()) {
            safeDeleteFile(gym.getCoverImageUrl());
            gym.setCoverImageUrl(null);
            gymRepository.save(gym);
        }
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages"}, key = "#gymId")
    public GymImageDto putGymImage(Long gymId, String imageName, String url) {
        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));
        GymImage gymImage = new GymImage();
        gymImage.setGym(gym);
        gymImage.setImageName(imageName);
        gymImage.setUrl(url);
        gymImage = gymImageRepository.save(gymImage);
        return GymImageDto.builder().id(gymImage.getId()).gymId(gymId).name(gymImage.getImageName()).url(gymImage.getUrl()).build();
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages"}, key = "#gymId")
    public GymImageDto uploadGymImage(Long gymId, String imageName, MultipartFile file) {
        validateImageFile(file);
        
        // This avoids fetching the entire images collection lazily, fixing the N+1 issue
        GymImage existingImage = gymImageRepository.findFirstByGymIdAndImageName(gymId, imageName).orElse(null);
        if (existingImage != null && existingImage.getUrl() != null && !existingImage.getUrl().isBlank()) {
            safeDeleteFile(existingImage.getUrl());
            gymImageRepository.delete(existingImage);
        }
        
        String fsId = fileStorageService.saveFile(file, "/gyms/" + gymId);
        String fullUrl = "/api/v1/media/stream/" + fsId;
        return putGymImage(gymId, imageName, fullUrl);
    }

    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages"}, key = "#gymId")
    public GymImageDto uploadRoomImage(Long gymId, String roomName, MultipartFile file) {
        validateImageFile(file);

        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_NOT_FOUND", "Gym not found"));

        String fsId = fileStorageService.saveFile(file, "/gyms/" + gymId);
        String fullUrl = "/api/v1/media/stream/" + fsId;

        GymImage gi = new GymImage();
        gi.setGym(gym);
        gi.setImageName(roomName); // The entity field is still 'imageName'
        gi.setUrl(fullUrl);
        gi.setType("photo");
        gi.setTitle(sanitizeFilename(file.getOriginalFilename()));
        gi = gymImageRepository.save(gi);

        return GymImageDto.builder().id(gi.getId()).gymId(gymId).name(gi.getImageName()).url(gi.getUrl()).build();
    }


    @Transactional
    @CacheEvict(cacheNames = {"gyms", "gymImages"}, key = "#gymId")
    public void deleteRoomImage(Long gymId, Long imageId) {
        GymImage existing = gymImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("GYM_IMAGE_NOT_FOUND", "Gym image not found"));
        if (existing.getGym() == null || !existing.getGym().getId().equals(gymId)) {
            throw new ResourceNotFoundException("GYM_IMAGE_MISMATCH", "Image does not belong to specified gym");
        }
        if (existing.getUrl() != null && !existing.getUrl().isBlank()) {
            safeDeleteFile(existing.getUrl());
        }
        gymImageRepository.delete(existing);
    }

    private void safeDeleteFile(String url) {
        try {
            fileStorageService.deleteFile(url);
        } catch (Exception e) {
            // Background error on deletion - swallowed on purpose
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("FILE_EMPTY", "File cannot be empty");
        }
        long maxSize = 5 * 1024 * 1024; // 5 MB
        if (file.getSize() > maxSize) {
            throw new BadRequestException("FILE_TOO_LARGE", "File size exceeds 5MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
            throw new BadRequestException("INVALID_FILE_TYPE", "Only JPG, PNG and WEBP images are allowed");
        }
        // Further validation reading Magic Bytes could be done here with Tika.
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }
}

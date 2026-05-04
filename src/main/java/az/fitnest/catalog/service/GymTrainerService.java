package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.response.GymTrainerResponse;
import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.request.TrainerRequest;
import az.fitnest.catalog.dto.request.TrainerAvailabilityRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface GymTrainerService {
    PaginatedResponse<GymTrainerResponse> getTrainers(Long gymId, int page, int pageSize, String sortDir);
    void addTrainer(Long gymId, TrainerRequest request);
    void updateTrainer(Long gymId, Long trainerId, TrainerRequest request);
    void deleteTrainer(Long gymId, Long trainerId);
    void updateTrainerPhoto(Long gymId, Long trainerId, MultipartFile file);
    void toggleTrainerReservation(Long gymId, Long trainerId, boolean enabled, Long lessonId);
    void addTrainerAvailability(Long gymId, Long trainerId, Long lessonId, TrainerAvailabilityRequest request);
    void addTrainers(Long gymId, List<String> names, List<String> surnames, List<Long> professionIds, List<String> emails, List<String> phones, List<MultipartFile> photos);
}

package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.GymImageDto;
import org.springframework.web.multipart.MultipartFile;

public interface GymImageService {
    void updateCoverImageUrl(Long gymId, String url);
    void deleteCoverImageUrl(Long gymId);
    GymImageDto putGymImage(Long gymId, String imageName, String url);
    GymImageDto uploadRoomImage(Long gymId, String roomName, MultipartFile file);
    void deleteRoomImage(Long gymId, Long imageId);
}

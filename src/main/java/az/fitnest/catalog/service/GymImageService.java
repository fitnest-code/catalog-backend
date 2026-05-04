package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.response.GymImageDto;
import org.springframework.web.multipart.MultipartFile;

public interface GymImageService {
    void updateCoverImageUrl(Long gymId, String url);
    void deleteCoverImageUrl(Long gymId);
    GymImageDto putGymImage(Long gymId, String imageName, String url);
    GymImageDto uploadRoomImage(Long gymId, String roomName, MultipartFile file);
    void deleteRoomImage(Long gymId, Long imageId);
}

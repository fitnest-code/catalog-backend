package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GymService {
    private final GymReadService gymReadService;
    private final GymWriteService gymWriteService;

    @Autowired
    public GymService(GymReadService gymReadService, GymWriteService gymWriteService) {
        this.gymReadService = gymReadService;
        this.gymWriteService = gymWriteService;
    }

    public GymDetailResponse getGymDetail(Long userId, Long gymId) {
        return gymReadService.getGymDetail(userId, gymId);
    }

    public GymImageResponse getGymImages(Long gymId) {
        return gymReadService.getGymImages(gymId);
    }

    public void createGym(GymRequest request) {
        gymWriteService.createGym(request);
    }

    public void updateGym(Long gymId, GymRequest request) {
        gymWriteService.updateGym(gymId, request);
    }

}

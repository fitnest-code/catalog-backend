/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.web.multipart.MultipartFile
 */
package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.GymDetailResponse;
import az.fitnest.catalog.dto.GymImageDto;
import az.fitnest.catalog.dto.GymImageResponse;
import az.fitnest.catalog.dto.GymMainPageDto;
import az.fitnest.catalog.dto.GymNearbyResponseDto;
import az.fitnest.catalog.dto.GymPackageIncludesResponse;
import az.fitnest.catalog.dto.GymPackagesResponse;
import az.fitnest.catalog.dto.GymRequest;
import az.fitnest.catalog.dto.GymReviewsResponse;
import az.fitnest.catalog.dto.GymTrainersResponse;
import az.fitnest.catalog.dto.ReviewRequest;
import az.fitnest.catalog.dto.TrainerRequest;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface GymService {
    public GymDetailResponse getGymDetail(Long var1, Long var2);

    public GymImageResponse getGymImages(Long var1);

    public GymPackagesResponse getGymPackages(Long var1);

    public GymPackageIncludesResponse getPackageIncludes(Long var1, Long var2);

    public GymTrainersResponse getTrainers(Long var1, int var2, int var3);

    public GymReviewsResponse getReviews(Long var1, int var2, int var3, String var4);

    public void addReview(Long var1, Long var2, ReviewRequest var3);

    public void addTrainer(Long var1, TrainerRequest var2);

    public void updateTrainer(Long var1, Long var2, TrainerRequest var3);

    public void deleteTrainer(Long var1, Long var2);

    public Object getReservationRules(Long var1);

    public void createGym(GymRequest var1);

    public void updateGym(Long var1, GymRequest var2);

    public List<GymNearbyResponseDto> getNearbyGyms(double var1, double var3, double var5);

    public List<GymMainPageDto> getClosestGyms(String var1, int var2, int var3, Double var4, Double var5);

    public GymImageDto putGymImage(Long var1, String var2, String var3);

    public GymImageDto uploadGymImage(Long var1, String var2, MultipartFile var3);

    public void deleteGym(Long var1);

    public String getGymQrUrl(Long gymId);

    public void updateLogoUrl(Long var1, String var2);

    public void updateCoverImageUrl(Long var1, String var2);

    public List<GymImageDto> uploadRoomImages(Long var1, String var2, MultipartFile[] var3);

    public GymImageDto replaceRoomImage(Long var1, Long var2, MultipartFile var3);

    public void deleteRoomImage(Long var1, Long var2);
}


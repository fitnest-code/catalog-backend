package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.response.LandingGymCardResponse;
import az.fitnest.catalog.dto.response.LandingGymDetailResponse;
import az.fitnest.catalog.dto.response.LandingGymFiltersResponse;
import az.fitnest.catalog.dto.response.LandingStatsResponse;
import az.fitnest.catalog.dto.response.LandingStoreCardResponse;
import az.fitnest.catalog.dto.response.LandingStoreDetailResponse;
import az.fitnest.catalog.dto.response.LandingStoreFiltersResponse;

public interface LandingPublicService {
    LandingStatsResponse getStats();

    PaginatedResponse<LandingGymCardResponse> getHomeGyms();

    PaginatedResponse<LandingGymCardResponse> getGyms(int page, int pageSize, String q, String city, String rayon, String category, String membership);

    LandingGymFiltersResponse getGymFilters();

    LandingGymDetailResponse getGym(Long gymId);

    PaginatedResponse<LandingStoreCardResponse> getHomeStores();

    PaginatedResponse<LandingStoreCardResponse> getStores(
            int page, int pageSize, String q, String city, String rayon, String category, String membership);

    LandingStoreFiltersResponse getStoreFilters();

    LandingStoreDetailResponse getStore(Long storeId);

    boolean isPublicLandingMedia(String fileId);

    void streamPublicLandingMedia(String fileId, java.io.OutputStream outputStream);
}

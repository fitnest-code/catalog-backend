package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.PaginatedResponse;
import az.fitnest.catalog.dto.response.LandingGymCardV2Response;
import az.fitnest.catalog.dto.response.LandingGymDetailV2Response;
import az.fitnest.catalog.dto.response.LandingGymFiltersResponse;

public interface LandingPublicV2Service {

    PaginatedResponse<LandingGymCardV2Response> getGyms(
            int page, int pageSize, String q, String city, String rayon, String category, String membership);

    LandingGymFiltersResponse getGymFilters();

    LandingGymDetailV2Response getGym(Long gymId);
}

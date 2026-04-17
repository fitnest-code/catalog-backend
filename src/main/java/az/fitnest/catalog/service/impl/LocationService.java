package az.fitnest.catalog.service.impl;

import az.fitnest.catalog.dto.admin.AdminPanelGeocodingResponse;
import az.fitnest.catalog.dto.admin.CityDto;
import az.fitnest.catalog.dto.admin.DistrictDto;
import az.fitnest.catalog.model.entity.AddressAdminPanel;
import az.fitnest.catalog.repository.GymAdminPanelRepository;
import az.fitnest.catalog.service.ReverseGeocodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final GymAdminPanelRepository gymRepository;
    private final ReverseGeocodingService reverseGeocodingService;

    public List<CityDto> getCities() {
        List<String> cities = gymRepository.findCities();
        List<CityDto> result = new ArrayList<>();
        for (int i = 0; i < cities.size(); i++) {
            result.add(new CityDto((long) (i + 1), cities.get(i)));
        }
        return result;
    }

    public List<DistrictDto> getDistricts(Long cityId) {
        String cityName = getCities().stream()
                .filter(c -> c.id().equals(cityId))
                .map(CityDto::name)
                .findFirst()
                .orElse(null);

        if (cityName == null) return List.of();

        List<String> districts = gymRepository.findDistrictsByCity(cityName);
        List<DistrictDto> result = new ArrayList<>();
        for (int i = 0; i < districts.size(); i++) {
            result.add(new DistrictDto((long) (i + 1), districts.get(i)));
        }
        return result;
    }

    public String resolveCityName(Long cityId) {
        if (cityId == null) return null;
        return getCities().stream()
                .filter(c -> c.id().equals(cityId))
                .map(CityDto::name)
                .findFirst()
                .orElse(null);
    }

    public String resolveDistrictName(Long cityId, Long districtId) {
        if (cityId == null || districtId == null) return null;
        return getDistricts(cityId).stream()
                .filter(d -> d.id().equals(districtId))
                .map(DistrictDto::name)
                .findFirst()
                .orElse(null);
    }


    public void resolveAndSetLocation(AddressAdminPanel address, AdminPanelGeocodingResponse geocoding) {
        if (geocoding == null) return;
        if (StringUtils.hasText(geocoding.city())) {
            address.setCity(geocoding.city());
        }
        if (StringUtils.hasText(geocoding.district())) {
            address.setDistrict(geocoding.district());
        }
    }
}
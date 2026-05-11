package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.request.StoreStep2Request;
import az.fitnest.catalog.dto.request.StoreStep3Request;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface StoreAdminService {

    Long createMarketStep1(String name, MultipartFile photo);

    void createMarketStep2(Long id, StoreStep2Request request);

    void createMarketStep3(Long id, StoreStep3Request request);
}

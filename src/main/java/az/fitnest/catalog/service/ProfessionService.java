package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.response.ProfessionResponse;
import az.fitnest.catalog.dto.*;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.response.*;
import az.fitnest.catalog.dto.request.ProfessionRequest;
import java.util.List;

public interface ProfessionService {
    List<ProfessionResponse> getAllProfessions();
    ProfessionResponse getProfessionById(Long id);
    ProfessionResponse createProfession(ProfessionRequest request);
    ProfessionResponse updateProfession(Long id, ProfessionRequest request);
    void deleteProfession(Long id);
    void deleteAllProfessions();
}

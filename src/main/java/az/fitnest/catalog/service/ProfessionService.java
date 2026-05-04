package az.fitnest.catalog.service;

import az.fitnest.catalog.dto.ProfessionDto;
import az.fitnest.catalog.dto.ProfessionRequest;
import java.util.List;

public interface ProfessionService {
    List<ProfessionDto> getAllProfessions();
    ProfessionDto getProfessionById(Long id);
    ProfessionDto createProfession(ProfessionRequest request);
    ProfessionDto updateProfession(Long id, ProfessionRequest request);
    void deleteProfession(Long id);
    void deleteAllProfessions();
}

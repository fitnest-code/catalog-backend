package az.fitnest.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationEntryResponse {
    private List<ClassTypeDto> classTypes;
    private List<LocalDate> availableDates;
    private List<RuleDto> commonRules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassTypeDto {
        private Long id;
        private String name;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleDto {
        private String title;
        private String description;
    }
}

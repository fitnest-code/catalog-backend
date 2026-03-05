package az.fitnest.catalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ToggleSaveResponse(
    @JsonProperty("is_saved") boolean isSaved
) {}

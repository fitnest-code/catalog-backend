package az.fitnest.catalog.dto;

import az.fitnest.catalog.model.entity.StoreSocialLink;
import lombok.Builder;
import java.util.List;

@Builder
public record StoreSocialDto(
    List<StoreSocialLink> links
) {}

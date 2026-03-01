package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.StoreDiscountDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMainPageDto {
    private Long storeId;
    private String name;
    private String address;
    private String city;
    private String logoUrl;
    private String coverImageUrl;
    private List<StoreDiscountDto> discounts;
    private Boolean isSaved;
    private Double distanceKm;
    private StoreSocialDto social;
    private Boolean isNew;
}

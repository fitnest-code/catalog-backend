package az.fitnest.catalog.model.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressAdminPanel {
    private String addressText;
    private String city;
    private Long cityId;
    private Double latitude;
    private Double longitude;
    private String district;
    private Long districtId;
}
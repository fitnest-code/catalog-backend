package az.fitnest.catalog.model.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    private String addressText;
    private String city;
    private Double latitude;
    private Double longitude;
}

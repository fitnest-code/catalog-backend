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
    private Double altitude;

    public Address(String addressText, String city, Double latitude, Double longitude) {
        this.addressText = addressText;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = null;
    }
}

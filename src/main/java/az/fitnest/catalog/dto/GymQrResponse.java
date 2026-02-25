package az.fitnest.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class GymQrResponse {
    private String qrCodeUrl;

    public GymQrResponse() {}

    public GymQrResponse(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }
}

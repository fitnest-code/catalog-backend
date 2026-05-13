package az.fitnest.catalog.dto.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;
import java.util.Optional;

@Getter
@Schema(description = "Mağaza məlumatlarının yenilənməsi. Yalnız göndərilən sahələr yenilənir.")
public class StoreUpdateRequest {

    @Schema(description = "Mağazanın adı", example = "FitLife Market")
    @Size(min = 2, max = 100, message = "Mağaza adı 2-100 simvol arasında olmalıdır")
    private Optional<String> name = Optional.empty();

    @Schema(description = "Mağazanın coğrafi enliyi", example = "40.4093")
    private Optional<Double> latitude = Optional.empty();

    @Schema(description = "Mağazanın coğrafi uzunluğu", example = "49.8671")
    private Optional<Double> longitude = Optional.empty();

    @Schema(description = "Mağazanın əlaqə nömrəsi", example = "+994501234567")
    private Optional<String> phone = Optional.empty();

    @Email(message = "Düzgün email formatı daxil edin")
    @Schema(description = "Mağazanın email ünvanı", example = "market@example.com")
    private Optional<String> email = Optional.empty();

    @Schema(
            description = "Mağazanın sosial media linki. null göndərildikdə silinir.",
            example = "https://instagram.com/market",
            nullable = true
    )
    private Optional<String> socialUrl = Optional.empty();
    private boolean socialUrlProvided = false;

    @Valid
    @Schema(
            description = "Mağazanın iş saatları. null göndərildikdə silinir.",
            nullable = true
    )
    private Optional<StoreWorkHoursRequest> workHours = Optional.empty();
    private boolean workHoursProvided = false;

    @Valid
    @Schema(
            description = "Paket endirimləri. Boş siyahı ([]) göndərildikdə bütün endirimler silinir. " +
                    "Sahə göndərilmədikdə mövcud endirimler saxlanır."
    )
    private Optional<List<DiscountItemRequest>> discounts = Optional.empty();

    @JsonSetter("name")
    public void setName(String name) {
        this.name = Optional.ofNullable(name);
    }

    @JsonSetter("latitude")
    public void setLatitude(Double latitude) {
        this.latitude = Optional.ofNullable(latitude);
    }

    @JsonSetter("longitude")
    public void setLongitude(Double longitude) {
        this.longitude = Optional.ofNullable(longitude);
    }

    @JsonSetter("phone")
    public void setPhone(String phone) {
        this.phone = Optional.ofNullable(phone);
    }

    @JsonSetter("email")
    public void setEmail(String email) {
        this.email = Optional.ofNullable(email);
    }

    @JsonSetter("socialUrl")
    public void setSocialUrl(String socialUrl) {
        this.socialUrl = Optional.ofNullable(socialUrl);
        this.socialUrlProvided = true;
    }

    @JsonSetter("workHours")
    public void setWorkHours(StoreWorkHoursRequest workHours) {
        this.workHours = Optional.ofNullable(workHours);
        this.workHoursProvided = true;
    }

    @JsonSetter(value = "discounts", nulls = Nulls.SKIP)
    public void setDiscounts(List<DiscountItemRequest> discounts) {
        this.discounts = Optional.ofNullable(discounts);
    }

    public boolean isSocialUrlProvided() {
        return socialUrlProvided;
    }

    public boolean isWorkHoursProvided() {
        return workHoursProvided;
    }
}

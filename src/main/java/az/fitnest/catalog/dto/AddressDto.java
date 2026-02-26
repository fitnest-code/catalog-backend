/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.annotation.JsonProperty
 *  com.fasterxml.jackson.annotation.JsonProperty$Access
 *  com.fasterxml.jackson.databind.annotation.JsonDeserialize
 *  io.swagger.v3.oas.annotations.media.Schema
 *  io.swagger.v3.oas.annotations.media.Schema$AccessMode
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.AddressDtoDeserializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@JsonDeserialize(using=AddressDtoDeserializer.class)
public class AddressDto {
    @JsonProperty(access=JsonProperty.Access.READ_ONLY)
    @Schema(description="Resolved address text", example="Baku, 28 May str. 12", accessMode=Schema.AccessMode.READ_ONLY)
    private String addressText;
    @JsonProperty(access=JsonProperty.Access.READ_ONLY)
    @Schema(description="City", example="Baku", accessMode=Schema.AccessMode.READ_ONLY)
    private String city;
    @JsonProperty(access=JsonProperty.Access.WRITE_ONLY)
    @Schema(description="Latitude")
    @NotNull(message = "Latitude cannot be null")
    private Double latitude;
    @JsonProperty(access=JsonProperty.Access.WRITE_ONLY)
    @Schema(description="Longitude")
    @NotNull(message = "Longitude cannot be null")
    private Double longitude;

    public static AddressDtoBuilder builder() {
        return new AddressDtoBuilder();
    }

    public String getAddressText() {
        return this.addressText;
    }

    public String getCity() {
        return this.city;
    }

    public Double getLatitude() {
        return this.latitude;
    }

    public Double getLongitude() {
        return this.longitude;
    }

    @JsonProperty(access=JsonProperty.Access.READ_ONLY)
    public void setAddressText(String addressText) {
        this.addressText = addressText;
    }

    @JsonProperty(access=JsonProperty.Access.READ_ONLY)
    public void setCity(String city) {
        this.city = city;
    }

    @JsonProperty(access=JsonProperty.Access.WRITE_ONLY)
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    @JsonProperty(access=JsonProperty.Access.WRITE_ONLY)
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AddressDto)) {
            return false;
        }
        AddressDto other = (AddressDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Double this$latitude = this.getLatitude();
        Double other$latitude = other.getLatitude();
        if (this$latitude == null ? other$latitude != null : !((Object)this$latitude).equals(other$latitude)) {
            return false;
        }
        Double this$longitude = this.getLongitude();
        Double other$longitude = other.getLongitude();
        if (this$longitude == null ? other$longitude != null : !((Object)this$longitude).equals(other$longitude)) {
            return false;
        }
        String this$addressText = this.getAddressText();
        String other$addressText = other.getAddressText();
        return !(this$addressText == null ? other$addressText != null : !this$addressText.equals(other$addressText));
    }

    protected boolean canEqual(Object other) {
        return other instanceof AddressDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Double $latitude = this.getLatitude();
        result = result * 59 + ($latitude == null ? 43 : ((Object)$latitude).hashCode());
        Double $longitude = this.getLongitude();
        result = result * 59 + ($longitude == null ? 43 : ((Object)$longitude).hashCode());
        String $addressText = this.getAddressText();
        result = result * 59 + ($addressText == null ? 43 : $addressText.hashCode());
        return result;
    }

    public String toString() {
        return "AddressDto(addressText=" + this.getAddressText() + ", city=" + this.getCity() + ", latitude=" + this.getLatitude() + ", longitude=" + this.getLongitude() + ")";
    }

    public AddressDto() {
    }

    public AddressDto(String addressText, String city, Double latitude, Double longitude) {
        this.addressText = addressText;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static class AddressDtoBuilder {
        private String addressText;
        private String city;
        private Double latitude;
        private Double longitude;

        AddressDtoBuilder() {
        }

        @JsonProperty(access=JsonProperty.Access.READ_ONLY)
        public AddressDtoBuilder addressText(String addressText) {
            this.addressText = addressText;
            return this;
        }

        @JsonProperty(access=JsonProperty.Access.READ_ONLY)
        public AddressDtoBuilder city(String city) {
            this.city = city;
            return this;
        }

        @JsonProperty(access=JsonProperty.Access.WRITE_ONLY)
        public AddressDtoBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        @JsonProperty(access=JsonProperty.Access.WRITE_ONLY)
        public AddressDtoBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public AddressDto build() {
            return new AddressDto(this.addressText, this.city, this.latitude, this.longitude);
        }

        public String toString() {
            return "AddressDto.AddressDtoBuilder(addressText=" + this.addressText + ", latitude=" + this.latitude + ", longitude=" + this.longitude + ")";
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Embeddable
 */
package az.fitnest.catalog.model.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class StoreAddress {
    private String addressText;
    private Double latitude;
    private Double longitude;

    public String getAddressText() {
        return this.addressText;
    }

    public Double getLatitude() {
        return this.latitude;
    }

    public Double getLongitude() {
        return this.longitude;
    }

    public void setAddressText(String addressText) {
        this.addressText = addressText;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StoreAddress)) {
            return false;
        }
        StoreAddress other = (StoreAddress)o;
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
        return other instanceof StoreAddress;
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
        return "StoreAddress(addressText=" + this.getAddressText() + ", latitude=" + this.getLatitude() + ", longitude=" + this.getLongitude() + ")";
    }

    public StoreAddress() {
    }

    public StoreAddress(String addressText, Double latitude, Double longitude) {
        this.addressText = addressText;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}


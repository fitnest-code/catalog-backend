/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.StoreDiscountDto;
import java.util.List;

public class StoreMainPageDto {
    private String storeId;
    private String name;
    private String address;
    private String logoUrl;
    private List<StoreDiscountDto> discounts;
    private Boolean isSaved;
    private Double distanceKm;
    private Boolean isNew;

    public static StoreMainPageDtoBuilder builder() {
        return new StoreMainPageDtoBuilder();
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getName() {
        return this.name;
    }

    public String getAddress() {
        return this.address;
    }

    public String getLogoUrl() {
        return this.logoUrl;
    }

    public List<StoreDiscountDto> getDiscounts() {
        return this.discounts;
    }

    public Boolean getIsSaved() {
        return this.isSaved;
    }

    public Double getDistanceKm() {
        return this.distanceKm;
    }

    public Boolean getIsNew() {
        return this.isNew;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setDiscounts(List<StoreDiscountDto> discounts) {
        this.discounts = discounts;
    }

    public void setIsSaved(Boolean isSaved) {
        this.isSaved = isSaved;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StoreMainPageDto)) {
            return false;
        }
        StoreMainPageDto other = (StoreMainPageDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$isSaved = this.getIsSaved();
        Boolean other$isSaved = other.getIsSaved();
        if (this$isSaved == null ? other$isSaved != null : !((Object)this$isSaved).equals(other$isSaved)) {
            return false;
        }
        Double this$distanceKm = this.getDistanceKm();
        Double other$distanceKm = other.getDistanceKm();
        if (this$distanceKm == null ? other$distanceKm != null : !((Object)this$distanceKm).equals(other$distanceKm)) {
            return false;
        }
        Boolean this$isNew = this.getIsNew();
        Boolean other$isNew = other.getIsNew();
        if (this$isNew == null ? other$isNew != null : !((Object)this$isNew).equals(other$isNew)) {
            return false;
        }
        String this$storeId = this.getStoreId();
        String other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$address = this.getAddress();
        String other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) {
            return false;
        }
        String this$logoUrl = this.getLogoUrl();
        String other$logoUrl = other.getLogoUrl();
        if (this$logoUrl == null ? other$logoUrl != null : !this$logoUrl.equals(other$logoUrl)) {
            return false;
        }
        List<StoreDiscountDto> this$discounts = this.getDiscounts();
        List<StoreDiscountDto> other$discounts = other.getDiscounts();
        return !(this$discounts == null ? other$discounts != null : !((Object)this$discounts).equals(other$discounts));
    }

    protected boolean canEqual(Object other) {
        return other instanceof StoreMainPageDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $isSaved = this.getIsSaved();
        result = result * 59 + ($isSaved == null ? 43 : ((Object)$isSaved).hashCode());
        Double $distanceKm = this.getDistanceKm();
        result = result * 59 + ($distanceKm == null ? 43 : ((Object)$distanceKm).hashCode());
        Boolean $isNew = this.getIsNew();
        result = result * 59 + ($isNew == null ? 43 : ((Object)$isNew).hashCode());
        String $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $address = this.getAddress();
        result = result * 59 + ($address == null ? 43 : $address.hashCode());
        String $logoUrl = this.getLogoUrl();
        result = result * 59 + ($logoUrl == null ? 43 : $logoUrl.hashCode());
        List<StoreDiscountDto> $discounts = this.getDiscounts();
        result = result * 59 + ($discounts == null ? 43 : ((Object)$discounts).hashCode());
        return result;
    }

    public String toString() {
        return "StoreMainPageDto(storeId=" + this.getStoreId() + ", name=" + this.getName() + ", address=" + this.getAddress() + ", logoUrl=" + this.getLogoUrl() + ", discounts=" + this.getDiscounts() + ", isSaved=" + this.getIsSaved() + ", distanceKm=" + this.getDistanceKm() + ", isNew=" + this.getIsNew() + ")";
    }

    public StoreMainPageDto() {
    }

    public StoreMainPageDto(String storeId, String name, String address, String logoUrl, List<StoreDiscountDto> discounts, Boolean isSaved, Double distanceKm, Boolean isNew) {
        this.storeId = storeId;
        this.name = name;
        this.address = address;
        this.logoUrl = logoUrl;
        this.discounts = discounts;
        this.isSaved = isSaved;
        this.distanceKm = distanceKm;
        this.isNew = isNew;
    }

    public static class StoreMainPageDtoBuilder {
        private String storeId;
        private String name;
        private String address;
        private String logoUrl;
        private List<StoreDiscountDto> discounts;
        private Boolean isSaved;
        private Double distanceKm;
        private Boolean isNew;

        StoreMainPageDtoBuilder() {
        }

        public StoreMainPageDtoBuilder storeId(String storeId) {
            this.storeId = storeId;
            return this;
        }

        public StoreMainPageDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public StoreMainPageDtoBuilder address(String address) {
            this.address = address;
            return this;
        }

        public StoreMainPageDtoBuilder logoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
            return this;
        }

        public StoreMainPageDtoBuilder discounts(List<StoreDiscountDto> discounts) {
            this.discounts = discounts;
            return this;
        }

        public StoreMainPageDtoBuilder isSaved(Boolean isSaved) {
            this.isSaved = isSaved;
            return this;
        }

        public StoreMainPageDtoBuilder distanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        public StoreMainPageDtoBuilder isNew(Boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        public StoreMainPageDto build() {
            return new StoreMainPageDto(this.storeId, this.name, this.address, this.logoUrl, this.discounts, this.isSaved, this.distanceKm, this.isNew);
        }

        public String toString() {
            return "StoreMainPageDto.StoreMainPageDtoBuilder(storeId=" + this.storeId + ", name=" + this.name + ", address=" + this.address + ", logoUrl=" + this.logoUrl + ", discounts=" + this.discounts + ", isSaved=" + this.isSaved + ", distanceKm=" + this.distanceKm + ", isNew=" + this.isNew + ")";
        }
    }
}


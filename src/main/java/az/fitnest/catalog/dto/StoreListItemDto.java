/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.StoreDiscountDto;
import java.util.List;

public class StoreListItemDto {
    private String storeId;
    private String name;
    private String description;
    private String address;
    private List<StoreDiscountDto> discounts;
    private String logoUrl;
    private String coverImageUrl;
    private Boolean isSaved;
    private Double distanceKm;
    private List<String> badges;
    private Boolean isNew;

    public static StoreListItemDtoBuilder builder() {
        return new StoreListItemDtoBuilder();
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getAddress() {
        return this.address;
    }

    public List<StoreDiscountDto> getDiscounts() {
        return this.discounts;
    }

    public String getLogoUrl() {
        return this.logoUrl;
    }

    public String getCoverImageUrl() {
        return this.coverImageUrl;
    }

    public Boolean getIsSaved() {
        return this.isSaved;
    }

    public Double getDistanceKm() {
        return this.distanceKm;
    }

    public List<String> getBadges() {
        return this.badges;
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

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setDiscounts(List<StoreDiscountDto> discounts) {
        this.discounts = discounts;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public void setIsSaved(Boolean isSaved) {
        this.isSaved = isSaved;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public void setBadges(List<String> badges) {
        this.badges = badges;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StoreListItemDto)) {
            return false;
        }
        StoreListItemDto other = (StoreListItemDto)o;
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
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$address = this.getAddress();
        String other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) {
            return false;
        }
        List<StoreDiscountDto> this$discounts = this.getDiscounts();
        List<StoreDiscountDto> other$discounts = other.getDiscounts();
        if (this$discounts == null ? other$discounts != null : !((Object)this$discounts).equals(other$discounts)) {
            return false;
        }
        String this$logoUrl = this.getLogoUrl();
        String other$logoUrl = other.getLogoUrl();
        if (this$logoUrl == null ? other$logoUrl != null : !this$logoUrl.equals(other$logoUrl)) {
            return false;
        }
        String this$coverImageUrl = this.getCoverImageUrl();
        String other$coverImageUrl = other.getCoverImageUrl();
        if (this$coverImageUrl == null ? other$coverImageUrl != null : !this$coverImageUrl.equals(other$coverImageUrl)) {
            return false;
        }
        List<String> this$badges = this.getBadges();
        List<String> other$badges = other.getBadges();
        return !(this$badges == null ? other$badges != null : !((Object)this$badges).equals(other$badges));
    }

    protected boolean canEqual(Object other) {
        return other instanceof StoreListItemDto;
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
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $address = this.getAddress();
        result = result * 59 + ($address == null ? 43 : $address.hashCode());
        List<StoreDiscountDto> $discounts = this.getDiscounts();
        result = result * 59 + ($discounts == null ? 43 : ((Object)$discounts).hashCode());
        String $logoUrl = this.getLogoUrl();
        result = result * 59 + ($logoUrl == null ? 43 : $logoUrl.hashCode());
        String $coverImageUrl = this.getCoverImageUrl();
        result = result * 59 + ($coverImageUrl == null ? 43 : $coverImageUrl.hashCode());
        List<String> $badges = this.getBadges();
        result = result * 59 + ($badges == null ? 43 : ((Object)$badges).hashCode());
        return result;
    }

    public String toString() {
        return "StoreListItemDto(storeId=" + this.getStoreId() + ", name=" + this.getName() + ", description=" + this.getDescription() + ", address=" + this.getAddress() + ", discounts=" + this.getDiscounts() + ", logoUrl=" + this.getLogoUrl() + ", coverImageUrl=" + this.getCoverImageUrl() + ", isSaved=" + this.getIsSaved() + ", distanceKm=" + this.getDistanceKm() + ", badges=" + this.getBadges() + ", isNew=" + this.getIsNew() + ")";
    }

    public StoreListItemDto() {
    }

    public StoreListItemDto(String storeId, String name, String description, String address, List<StoreDiscountDto> discounts, String logoUrl, String coverImageUrl, Boolean isSaved, Double distanceKm, List<String> badges, Boolean isNew) {
        this.storeId = storeId;
        this.name = name;
        this.description = description;
        this.address = address;
        this.discounts = discounts;
        this.logoUrl = logoUrl;
        this.coverImageUrl = coverImageUrl;
        this.isSaved = isSaved;
        this.distanceKm = distanceKm;
        this.badges = badges;
        this.isNew = isNew;
    }

    public static class StoreListItemDtoBuilder {
        private String storeId;
        private String name;
        private String description;
        private String address;
        private List<StoreDiscountDto> discounts;
        private String logoUrl;
        private String coverImageUrl;
        private Boolean isSaved;
        private Double distanceKm;
        private List<String> badges;
        private Boolean isNew;

        StoreListItemDtoBuilder() {
        }

        public StoreListItemDtoBuilder storeId(String storeId) {
            this.storeId = storeId;
            return this;
        }

        public StoreListItemDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public StoreListItemDtoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public StoreListItemDtoBuilder address(String address) {
            this.address = address;
            return this;
        }

        public StoreListItemDtoBuilder discounts(List<StoreDiscountDto> discounts) {
            this.discounts = discounts;
            return this;
        }

        public StoreListItemDtoBuilder logoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
            return this;
        }

        public StoreListItemDtoBuilder coverImageUrl(String coverImageUrl) {
            this.coverImageUrl = coverImageUrl;
            return this;
        }

        public StoreListItemDtoBuilder isSaved(Boolean isSaved) {
            this.isSaved = isSaved;
            return this;
        }

        public StoreListItemDtoBuilder distanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        public StoreListItemDtoBuilder badges(List<String> badges) {
            this.badges = badges;
            return this;
        }

        public StoreListItemDtoBuilder isNew(Boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        public StoreListItemDto build() {
            return new StoreListItemDto(this.storeId, this.name, this.description, this.address, this.discounts, this.logoUrl, this.coverImageUrl, this.isSaved, this.distanceKm, this.badges, this.isNew);
        }

        public String toString() {
            return "StoreListItemDto.StoreListItemDtoBuilder(storeId=" + this.storeId + ", name=" + this.name + ", description=" + this.description + ", address=" + this.address + ", discounts=" + this.discounts + ", logoUrl=" + this.logoUrl + ", coverImageUrl=" + this.coverImageUrl + ", isSaved=" + this.isSaved + ", distanceKm=" + this.distanceKm + ", badges=" + this.badges + ", isNew=" + this.isNew + ")";
        }
    }
}


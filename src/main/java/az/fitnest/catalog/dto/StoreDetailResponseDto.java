/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.AddressDto;
import az.fitnest.catalog.dto.StoreDiscountDto;
import az.fitnest.catalog.dto.StoreSocialDto;
import az.fitnest.catalog.dto.StoreWorkHourDto;
import java.util.List;

public class StoreDetailResponseDto {
    private String storeId;
    private String name;
    private String description;
    private AddressDto address;
    private String phone;
    private String category;
    private String status;
    private List<StoreWorkHourDto> workingHours;
    private List<StoreDiscountDto> discounts;
    private StoreSocialDto social;
    private List<String> images;
    private Boolean isSaved;
    private Boolean isNew;

    public static StoreDetailResponseDtoBuilder builder() {
        return new StoreDetailResponseDtoBuilder();
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

    public AddressDto getAddress() {
        return this.address;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getCategory() {
        return this.category;
    }

    public String getStatus() {
        return this.status;
    }

    public List<StoreWorkHourDto> getWorkingHours() {
        return this.workingHours;
    }

    public List<StoreDiscountDto> getDiscounts() {
        return this.discounts;
    }

    public StoreSocialDto getSocial() {
        return this.social;
    }

    public List<String> getImages() {
        return this.images;
    }

    public Boolean getIsSaved() {
        return this.isSaved;
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

    public void setAddress(AddressDto address) {
        this.address = address;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setWorkingHours(List<StoreWorkHourDto> workingHours) {
        this.workingHours = workingHours;
    }

    public void setDiscounts(List<StoreDiscountDto> discounts) {
        this.discounts = discounts;
    }

    public void setSocial(StoreSocialDto social) {
        this.social = social;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public void setIsSaved(Boolean isSaved) {
        this.isSaved = isSaved;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StoreDetailResponseDto)) {
            return false;
        }
        StoreDetailResponseDto other = (StoreDetailResponseDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Boolean this$isSaved = this.getIsSaved();
        Boolean other$isSaved = other.getIsSaved();
        if (this$isSaved == null ? other$isSaved != null : !((Object)this$isSaved).equals(other$isSaved)) {
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
        AddressDto this$address = this.getAddress();
        AddressDto other$address = other.getAddress();
        if (this$address == null ? other$address != null : !((Object)this$address).equals(other$address)) {
            return false;
        }
        String this$phone = this.getPhone();
        String other$phone = other.getPhone();
        if (this$phone == null ? other$phone != null : !this$phone.equals(other$phone)) {
            return false;
        }
        String this$category = this.getCategory();
        String other$category = other.getCategory();
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        List<StoreWorkHourDto> this$workingHours = this.getWorkingHours();
        List<StoreWorkHourDto> other$workingHours = other.getWorkingHours();
        if (this$workingHours == null ? other$workingHours != null : !((Object)this$workingHours).equals(other$workingHours)) {
            return false;
        }
        List<StoreDiscountDto> this$discounts = this.getDiscounts();
        List<StoreDiscountDto> other$discounts = other.getDiscounts();
        if (this$discounts == null ? other$discounts != null : !((Object)this$discounts).equals(other$discounts)) {
            return false;
        }
        StoreSocialDto this$social = this.getSocial();
        StoreSocialDto other$social = other.getSocial();
        if (this$social == null ? other$social != null : !((Object)this$social).equals(other$social)) {
            return false;
        }
        List<String> this$images = this.getImages();
        List<String> other$images = other.getImages();
        return !(this$images == null ? other$images != null : !((Object)this$images).equals(other$images));
    }

    protected boolean canEqual(Object other) {
        return other instanceof StoreDetailResponseDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Boolean $isSaved = this.getIsSaved();
        result = result * 59 + ($isSaved == null ? 43 : ((Object)$isSaved).hashCode());
        Boolean $isNew = this.getIsNew();
        result = result * 59 + ($isNew == null ? 43 : ((Object)$isNew).hashCode());
        String $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        AddressDto $address = this.getAddress();
        result = result * 59 + ($address == null ? 43 : ((Object)$address).hashCode());
        String $phone = this.getPhone();
        result = result * 59 + ($phone == null ? 43 : $phone.hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        List<StoreWorkHourDto> $workingHours = this.getWorkingHours();
        result = result * 59 + ($workingHours == null ? 43 : ((Object)$workingHours).hashCode());
        List<StoreDiscountDto> $discounts = this.getDiscounts();
        result = result * 59 + ($discounts == null ? 43 : ((Object)$discounts).hashCode());
        StoreSocialDto $social = this.getSocial();
        result = result * 59 + ($social == null ? 43 : ((Object)$social).hashCode());
        List<String> $images = this.getImages();
        result = result * 59 + ($images == null ? 43 : ((Object)$images).hashCode());
        return result;
    }

    public String toString() {
        return "StoreDetailResponseDto(storeId=" + this.getStoreId() + ", name=" + this.getName() + ", description=" + this.getDescription() + ", address=" + this.getAddress() + ", phone=" + this.getPhone() + ", category=" + this.getCategory() + ", status=" + this.getStatus() + ", workingHours=" + this.getWorkingHours() + ", discounts=" + this.getDiscounts() + ", social=" + this.getSocial() + ", images=" + this.getImages() + ", isSaved=" + this.getIsSaved() + ", isNew=" + this.getIsNew() + ")";
    }

    public StoreDetailResponseDto() {
    }

    public StoreDetailResponseDto(String storeId, String name, String description, AddressDto address, String phone, String category, String status, List<StoreWorkHourDto> workingHours, List<StoreDiscountDto> discounts, StoreSocialDto social, List<String> images, Boolean isSaved, Boolean isNew) {
        this.storeId = storeId;
        this.name = name;
        this.description = description;
        this.address = address;
        this.phone = phone;
        this.category = category;
        this.status = status;
        this.workingHours = workingHours;
        this.discounts = discounts;
        this.social = social;
        this.images = images;
        this.isSaved = isSaved;
        this.isNew = isNew;
    }

    public static class StoreDetailResponseDtoBuilder {
        private String storeId;
        private String name;
        private String description;
        private AddressDto address;
        private String phone;
        private String category;
        private String status;
        private List<StoreWorkHourDto> workingHours;
        private List<StoreDiscountDto> discounts;
        private StoreSocialDto social;
        private List<String> images;
        private Boolean isSaved;
        private Boolean isNew;

        StoreDetailResponseDtoBuilder() {
        }

        public StoreDetailResponseDtoBuilder storeId(String storeId) {
            this.storeId = storeId;
            return this;
        }

        public StoreDetailResponseDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public StoreDetailResponseDtoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public StoreDetailResponseDtoBuilder address(AddressDto address) {
            this.address = address;
            return this;
        }

        public StoreDetailResponseDtoBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public StoreDetailResponseDtoBuilder category(String category) {
            this.category = category;
            return this;
        }

        public StoreDetailResponseDtoBuilder status(String status) {
            this.status = status;
            return this;
        }

        public StoreDetailResponseDtoBuilder workingHours(List<StoreWorkHourDto> workingHours) {
            this.workingHours = workingHours;
            return this;
        }

        public StoreDetailResponseDtoBuilder discounts(List<StoreDiscountDto> discounts) {
            this.discounts = discounts;
            return this;
        }

        public StoreDetailResponseDtoBuilder social(StoreSocialDto social) {
            this.social = social;
            return this;
        }

        public StoreDetailResponseDtoBuilder images(List<String> images) {
            this.images = images;
            return this;
        }

        public StoreDetailResponseDtoBuilder isSaved(Boolean isSaved) {
            this.isSaved = isSaved;
            return this;
        }

        public StoreDetailResponseDtoBuilder isNew(Boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        public StoreDetailResponseDto build() {
            return new StoreDetailResponseDto(this.storeId, this.name, this.description, this.address, this.phone, this.category, this.status, this.workingHours, this.discounts, this.social, this.images, this.isSaved, this.isNew);
        }

        public String toString() {
            return "StoreDetailResponseDto.StoreDetailResponseDtoBuilder(storeId=" + this.storeId + ", name=" + this.name + ", description=" + this.description + ", address=" + this.address + ", phone=" + this.phone + ", category=" + this.category + ", status=" + this.status + ", workingHours=" + this.workingHours + ", discounts=" + this.discounts + ", social=" + this.social + ", images=" + this.images + ", isSaved=" + this.isSaved + ", isNew=" + this.isNew + ")";
        }
    }
}


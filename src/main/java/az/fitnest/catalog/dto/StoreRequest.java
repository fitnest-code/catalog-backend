/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  jakarta.validation.constraints.NotBlank
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.AddressDto;
import az.fitnest.catalog.dto.StoreWorkHourDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Schema(description="Request to create or update a store")
public class StoreRequest {
    @NotBlank
    @Schema(description="Store name", example="Fit Market")
    private String name;
    @Schema(description="Store description")
    private String description;
    @Schema(description="Store address")
    private AddressDto address;
    @Schema(description="Store phone")
    private String phone;
    @Schema(description="Store category", example="SUPPLEMENTS")
    private String category;
    @Schema(description="Store status", example="ACTIVE")
    private String status;
    @Schema(description="Store work hours")
    private List<StoreWorkHourDto> workingHours;
    @Schema(description="Logo URL")
    private String logoUrl;
    @Schema(description="Cover image URL")
    private String coverImageUrl;
    @Schema(description="List of badges")
    private List<String> badges;

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

    public String getLogoUrl() {
        return this.logoUrl;
    }

    public String getCoverImageUrl() {
        return this.coverImageUrl;
    }

    public List<String> getBadges() {
        return this.badges;
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

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public void setBadges(List<String> badges) {
        this.badges = badges;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StoreRequest)) {
            return false;
        }
        StoreRequest other = (StoreRequest)o;
        if (!other.canEqual(this)) {
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
        return other instanceof StoreRequest;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
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
        String $logoUrl = this.getLogoUrl();
        result = result * 59 + ($logoUrl == null ? 43 : $logoUrl.hashCode());
        String $coverImageUrl = this.getCoverImageUrl();
        result = result * 59 + ($coverImageUrl == null ? 43 : $coverImageUrl.hashCode());
        List<String> $badges = this.getBadges();
        result = result * 59 + ($badges == null ? 43 : ((Object)$badges).hashCode());
        return result;
    }

    public String toString() {
        return "StoreRequest(name=" + this.getName() + ", description=" + this.getDescription() + ", address=" + this.getAddress() + ", phone=" + this.getPhone() + ", category=" + this.getCategory() + ", status=" + this.getStatus() + ", workingHours=" + this.getWorkingHours() + ", logoUrl=" + this.getLogoUrl() + ", coverImageUrl=" + this.getCoverImageUrl() + ", badges=" + this.getBadges() + ")";
    }
}


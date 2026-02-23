/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.AddressDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

public class GymRequest {
    private String name;
    private String description;
    @Schema(description="Gym status", allowableValues={"ACTIVE", "INACTIVE"}, example="ACTIVE")
    private String status;
    private String coverImageUrl;
    private AddressDto address;
    private String phone;
    private String email;
    private Set<Long> categoryIds;

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getStatus() {
        return this.status;
    }

    public String getCoverImageUrl() {
        return this.coverImageUrl;
    }

    public AddressDto getAddress() {
        return this.address;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getEmail() {
        return this.email;
    }

    public Set<Long> getCategoryIds() {
        return this.categoryIds;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setCategoryIds(Set<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymRequest)) {
            return false;
        }
        GymRequest other = (GymRequest)o;
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
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$coverImageUrl = this.getCoverImageUrl();
        String other$coverImageUrl = other.getCoverImageUrl();
        if (this$coverImageUrl == null ? other$coverImageUrl != null : !this$coverImageUrl.equals(other$coverImageUrl)) {
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
        String this$email = this.getEmail();
        String other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) {
            return false;
        }
        Set<Long> this$categoryIds = this.getCategoryIds();
        Set<Long> other$categoryIds = other.getCategoryIds();
        return !(this$categoryIds == null ? other$categoryIds != null : !((Object)this$categoryIds).equals(other$categoryIds));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymRequest;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $coverImageUrl = this.getCoverImageUrl();
        result = result * 59 + ($coverImageUrl == null ? 43 : $coverImageUrl.hashCode());
        AddressDto $address = this.getAddress();
        result = result * 59 + ($address == null ? 43 : ((Object)$address).hashCode());
        String $phone = this.getPhone();
        result = result * 59 + ($phone == null ? 43 : $phone.hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        Set<Long> $categoryIds = this.getCategoryIds();
        result = result * 59 + ($categoryIds == null ? 43 : ((Object)$categoryIds).hashCode());
        return result;
    }

    public String toString() {
        return "GymRequest(name=" + this.getName() + ", description=" + this.getDescription() + ", status=" + this.getStatus() + ", coverImageUrl=" + this.getCoverImageUrl() + ", address=" + this.getAddress() + ", phone=" + this.getPhone() + ", email=" + this.getEmail() + ", categoryIds=" + this.getCategoryIds() + ")";
    }
}


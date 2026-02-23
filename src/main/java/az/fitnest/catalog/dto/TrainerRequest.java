/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 *  jakarta.validation.constraints.NotBlank
 */
package az.fitnest.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description="Request to create or update a trainer")
public class TrainerRequest {
    @NotBlank
    @Schema(description="Trainer's full name", example="John Doe")
    private String fullName;
    @Schema(description="Specialization", example="CrossFit, Yoga")
    private String specialization;
    @Schema(description="Profile image URL")
    private String imageUrl;

    public String getFullName() {
        return this.fullName;
    }

    public String getSpecialization() {
        return this.specialization;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TrainerRequest)) {
            return false;
        }
        TrainerRequest other = (TrainerRequest)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$fullName = this.getFullName();
        String other$fullName = other.getFullName();
        if (this$fullName == null ? other$fullName != null : !this$fullName.equals(other$fullName)) {
            return false;
        }
        String this$specialization = this.getSpecialization();
        String other$specialization = other.getSpecialization();
        if (this$specialization == null ? other$specialization != null : !this$specialization.equals(other$specialization)) {
            return false;
        }
        String this$imageUrl = this.getImageUrl();
        String other$imageUrl = other.getImageUrl();
        return !(this$imageUrl == null ? other$imageUrl != null : !this$imageUrl.equals(other$imageUrl));
    }

    protected boolean canEqual(Object other) {
        return other instanceof TrainerRequest;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $fullName = this.getFullName();
        result = result * 59 + ($fullName == null ? 43 : $fullName.hashCode());
        String $specialization = this.getSpecialization();
        result = result * 59 + ($specialization == null ? 43 : $specialization.hashCode());
        String $imageUrl = this.getImageUrl();
        result = result * 59 + ($imageUrl == null ? 43 : $imageUrl.hashCode());
        return result;
    }

    public String toString() {
        return "TrainerRequest(fullName=" + this.getFullName() + ", specialization=" + this.getSpecialization() + ", imageUrl=" + this.getImageUrl() + ")";
    }
}


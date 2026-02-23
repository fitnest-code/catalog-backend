/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

public class GymPlanBenefitDto {
    private String logo;
    private String description;

    public static GymPlanBenefitDtoBuilder builder() {
        return new GymPlanBenefitDtoBuilder();
    }

    public String getLogo() {
        return this.logo;
    }

    public String getDescription() {
        return this.description;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymPlanBenefitDto)) {
            return false;
        }
        GymPlanBenefitDto other = (GymPlanBenefitDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$logo = this.getLogo();
        String other$logo = other.getLogo();
        if (this$logo == null ? other$logo != null : !this$logo.equals(other$logo)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        return !(this$description == null ? other$description != null : !this$description.equals(other$description));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymPlanBenefitDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $logo = this.getLogo();
        result = result * 59 + ($logo == null ? 43 : $logo.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        return result;
    }

    public String toString() {
        return "GymPlanBenefitDto(logo=" + this.getLogo() + ", description=" + this.getDescription() + ")";
    }

    public GymPlanBenefitDto() {
    }

    public GymPlanBenefitDto(String logo, String description) {
        this.logo = logo;
        this.description = description;
    }

    public static class GymPlanBenefitDtoBuilder {
        private String logo;
        private String description;

        GymPlanBenefitDtoBuilder() {
        }

        public GymPlanBenefitDtoBuilder logo(String logo) {
            this.logo = logo;
            return this;
        }

        public GymPlanBenefitDtoBuilder description(String description) {
            this.description = description;
            return this;
        }

        public GymPlanBenefitDto build() {
            return new GymPlanBenefitDto(this.logo, this.description);
        }

        public String toString() {
            return "GymPlanBenefitDto.GymPlanBenefitDtoBuilder(logo=" + this.logo + ", description=" + this.description + ")";
        }
    }
}


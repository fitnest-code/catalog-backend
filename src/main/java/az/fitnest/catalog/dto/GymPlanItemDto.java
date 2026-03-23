package az.fitnest.catalog.dto;

import java.util.List;

public class GymPlanItemDto {
    private String plan_id;
    private String packageName;
    private List<String> benefits;

    public GymPlanItemDto() {
    }

    public GymPlanItemDto(String plan_id, String packageName, List<String> benefits) {
        this.plan_id = plan_id;
        this.packageName = packageName;
        this.benefits = benefits;
    }

    public static GymPlanItemDtoBuilder builder() {
        return new GymPlanItemDtoBuilder();
    }

    public String getPlan_id() {
        return this.plan_id;
    }

    public void setPlan_id(String plan_id) {
        this.plan_id = plan_id;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<String> getBenefits() {
        return this.benefits;
    }

    public void setBenefits(List<String> benefits) {
        this.benefits = benefits;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymPlanItemDto)) {
            return false;
        }
        GymPlanItemDto other = (GymPlanItemDto) o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$plan_id = this.getPlan_id();
        String other$plan_id = other.getPlan_id();
        if (this$plan_id == null ? other$plan_id != null : !this$plan_id.equals(other$plan_id)) {
            return false;
        }
        String this$packageName = this.getPackageName();
        String other$packageName = other.getPackageName();
        if (this$packageName == null ? other$packageName != null : !this$packageName.equals(other$packageName)) {
            return false;
        }
        List<String> this$benefits = this.getBenefits();
        List<String> other$benefits = other.getBenefits();
        return !(this$benefits == null ? other$benefits != null : !((Object) this$benefits).equals(other$benefits));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymPlanItemDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $plan_id = this.getPlan_id();
        result = result * 59 + ($plan_id == null ? 43 : $plan_id.hashCode());
        String $packageName = this.getPackageName();
        result = result * 59 + ($packageName == null ? 43 : $packageName.hashCode());
        List<String> $benefits = this.getBenefits();
        result = result * 59 + ($benefits == null ? 43 : ((Object) $benefits).hashCode());
        return result;
    }

    public String toString() {
        return "GymPlanItemDto(plan_id=" + this.getPlan_id() + ", packageName=" + this.getPackageName() + ", benefits=" + this.getBenefits() + ")";
    }

    public static class GymPlanItemDtoBuilder {
        private String plan_id;
        private String packageName;
        private List<String> benefits;

        GymPlanItemDtoBuilder() {
        }

        public GymPlanItemDtoBuilder plan_id(String plan_id) {
            this.plan_id = plan_id;
            return this;
        }

        public GymPlanItemDtoBuilder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public GymPlanItemDtoBuilder benefits(List<String> benefits) {
            this.benefits = benefits;
            return this;
        }

        public GymPlanItemDto build() {
            return new GymPlanItemDto(this.plan_id, this.packageName, this.benefits);
        }

        public String toString() {
            return "GymPlanItemDto.GymPlanItemDtoBuilder(plan_id=" + this.plan_id + ", packageName=" + this.packageName + ", benefits=" + this.benefits + ")";
        }
    }
}

/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import java.util.List;

public class GymPlanItemDto {
    private String plan_id;
    private String name;
    private List<String> benefits;

    public GymPlanItemDto() {
    }

    public GymPlanItemDto(String plan_id, String name, List<String> benefits) {
        this.plan_id = plan_id;
        this.name = name;
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

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
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
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
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
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        List<String> $benefits = this.getBenefits();
        result = result * 59 + ($benefits == null ? 43 : ((Object) $benefits).hashCode());
        return result;
    }

    public String toString() {
        return "GymPlanItemDto(plan_id=" + this.getPlan_id() + ", name=" + this.getName() + ", benefits=" + this.getBenefits() + ")";
    }

    public static class GymPlanItemDtoBuilder {
        private String plan_id;
        private String name;
        private List<String> benefits;

        GymPlanItemDtoBuilder() {
        }

        public GymPlanItemDtoBuilder plan_id(String plan_id) {
            this.plan_id = plan_id;
            return this;
        }

        public GymPlanItemDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public GymPlanItemDtoBuilder benefits(List<String> benefits) {
            this.benefits = benefits;
            return this;
        }

        public GymPlanItemDto build() {
            return new GymPlanItemDto(this.plan_id, this.name, this.benefits);
        }

        public String toString() {
            return "GymPlanItemDto.GymPlanItemDtoBuilder(plan_id=" + this.plan_id + ", name=" + this.name + ", benefits=" + this.benefits + ")";
        }
    }
}


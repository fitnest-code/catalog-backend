/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.GymPlanBenefitDto;
import java.util.List;

public class GymPackageIncludesResponse {
    private String plan_id;
    private List<GymPlanBenefitDto> items;

    public static GymPackageIncludesResponseBuilder builder() {
        return new GymPackageIncludesResponseBuilder();
    }

    public String getPlan_id() {
        return this.plan_id;
    }

    public List<GymPlanBenefitDto> getItems() {
        return this.items;
    }

    public void setPlan_id(String plan_id) {
        this.plan_id = plan_id;
    }

    public void setItems(List<GymPlanBenefitDto> items) {
        this.items = items;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymPackageIncludesResponse)) {
            return false;
        }
        GymPackageIncludesResponse other = (GymPackageIncludesResponse)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$plan_id = this.getPlan_id();
        String other$plan_id = other.getPlan_id();
        if (this$plan_id == null ? other$plan_id != null : !this$plan_id.equals(other$plan_id)) {
            return false;
        }
        List<GymPlanBenefitDto> this$items = this.getItems();
        List<GymPlanBenefitDto> other$items = other.getItems();
        return !(this$items == null ? other$items != null : !((Object)this$items).equals(other$items));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymPackageIncludesResponse;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $plan_id = this.getPlan_id();
        result = result * 59 + ($plan_id == null ? 43 : $plan_id.hashCode());
        List<GymPlanBenefitDto> $items = this.getItems();
        result = result * 59 + ($items == null ? 43 : ((Object)$items).hashCode());
        return result;
    }

    public String toString() {
        return "GymPackageIncludesResponse(plan_id=" + this.getPlan_id() + ", items=" + this.getItems() + ")";
    }

    public GymPackageIncludesResponse() {
    }

    public GymPackageIncludesResponse(String plan_id, List<GymPlanBenefitDto> items) {
        this.plan_id = plan_id;
        this.items = items;
    }

    public static class GymPackageIncludesResponseBuilder {
        private String plan_id;
        private List<GymPlanBenefitDto> items;

        GymPackageIncludesResponseBuilder() {
        }

        public GymPackageIncludesResponseBuilder plan_id(String plan_id) {
            this.plan_id = plan_id;
            return this;
        }

        public GymPackageIncludesResponseBuilder items(List<GymPlanBenefitDto> items) {
            this.items = items;
            return this;
        }

        public GymPackageIncludesResponse build() {
            return new GymPackageIncludesResponse(this.plan_id, this.items);
        }

        public String toString() {
            return "GymPackageIncludesResponse.GymPackageIncludesResponseBuilder(plan_id=" + this.plan_id + ", items=" + this.items + ")";
        }
    }
}


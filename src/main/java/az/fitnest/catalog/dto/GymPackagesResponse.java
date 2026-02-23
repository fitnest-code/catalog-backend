/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.GymPlanItemDto;
import java.util.List;

public class GymPackagesResponse {
    private List<GymPlanItemDto> items;

    public static GymPackagesResponseBuilder builder() {
        return new GymPackagesResponseBuilder();
    }

    public List<GymPlanItemDto> getItems() {
        return this.items;
    }

    public void setItems(List<GymPlanItemDto> items) {
        this.items = items;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymPackagesResponse)) {
            return false;
        }
        GymPackagesResponse other = (GymPackagesResponse)o;
        if (!other.canEqual(this)) {
            return false;
        }
        List<GymPlanItemDto> this$items = this.getItems();
        List<GymPlanItemDto> other$items = other.getItems();
        return !(this$items == null ? other$items != null : !((Object)this$items).equals(other$items));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymPackagesResponse;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        List<GymPlanItemDto> $items = this.getItems();
        result = result * 59 + ($items == null ? 43 : ((Object)$items).hashCode());
        return result;
    }

    public String toString() {
        return "GymPackagesResponse(items=" + this.getItems() + ")";
    }

    public GymPackagesResponse() {
    }

    public GymPackagesResponse(List<GymPlanItemDto> items) {
        this.items = items;
    }

    public static class GymPackagesResponseBuilder {
        private List<GymPlanItemDto> items;

        GymPackagesResponseBuilder() {
        }

        public GymPackagesResponseBuilder items(List<GymPlanItemDto> items) {
            this.items = items;
            return this;
        }

        public GymPackagesResponse build() {
            return new GymPackagesResponse(this.items);
        }

        public String toString() {
            return "GymPackagesResponse.GymPackagesResponseBuilder(items=" + this.items + ")";
        }
    }
}


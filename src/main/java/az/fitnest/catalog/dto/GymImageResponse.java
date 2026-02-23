/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.GymImageItemDto;
import java.util.List;

public class GymImageResponse {
    private List<GymImageItemDto> items;

    public static GymImageResponseBuilder builder() {
        return new GymImageResponseBuilder();
    }

    public List<GymImageItemDto> getItems() {
        return this.items;
    }

    public void setItems(List<GymImageItemDto> items) {
        this.items = items;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymImageResponse)) {
            return false;
        }
        GymImageResponse other = (GymImageResponse)o;
        if (!other.canEqual(this)) {
            return false;
        }
        List<GymImageItemDto> this$items = this.getItems();
        List<GymImageItemDto> other$items = other.getItems();
        return !(this$items == null ? other$items != null : !((Object)this$items).equals(other$items));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymImageResponse;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        List<GymImageItemDto> $items = this.getItems();
        result = result * 59 + ($items == null ? 43 : ((Object)$items).hashCode());
        return result;
    }

    public String toString() {
        return "GymImageResponse(items=" + this.getItems() + ")";
    }

    public GymImageResponse() {
    }

    public GymImageResponse(List<GymImageItemDto> items) {
        this.items = items;
    }

    public static class GymImageResponseBuilder {
        private List<GymImageItemDto> items;

        GymImageResponseBuilder() {
        }

        public GymImageResponseBuilder items(List<GymImageItemDto> items) {
            this.items = items;
            return this;
        }

        public GymImageResponse build() {
            return new GymImageResponse(this.items);
        }

        public String toString() {
            return "GymImageResponse.GymImageResponseBuilder(items=" + this.items + ")";
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.GymTrainerDto;
import java.util.List;

public class GymTrainersResponse {
    private List<GymTrainerDto> items;
    private long total;
    private int page;
    private int pageSize;

    public static GymTrainersResponseBuilder builder() {
        return new GymTrainersResponseBuilder();
    }

    public List<GymTrainerDto> getItems() {
        return this.items;
    }

    public long getTotal() {
        return this.total;
    }

    public int getPage() {
        return this.page;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public void setItems(List<GymTrainerDto> items) {
        this.items = items;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymTrainersResponse)) {
            return false;
        }
        GymTrainersResponse other = (GymTrainersResponse)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getTotal() != other.getTotal()) {
            return false;
        }
        if (this.getPage() != other.getPage()) {
            return false;
        }
        if (this.getPageSize() != other.getPageSize()) {
            return false;
        }
        List<GymTrainerDto> this$items = this.getItems();
        List<GymTrainerDto> other$items = other.getItems();
        return !(this$items == null ? other$items != null : !((Object)this$items).equals(other$items));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymTrainersResponse;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $total = this.getTotal();
        result = result * 59 + (int)($total >>> 32 ^ $total);
        result = result * 59 + this.getPage();
        result = result * 59 + this.getPageSize();
        List<GymTrainerDto> $items = this.getItems();
        result = result * 59 + ($items == null ? 43 : ((Object)$items).hashCode());
        return result;
    }

    public String toString() {
        return "GymTrainersResponse(items=" + this.getItems() + ", total=" + this.getTotal() + ", page=" + this.getPage() + ", pageSize=" + this.getPageSize() + ")";
    }

    public GymTrainersResponse() {
    }

    public GymTrainersResponse(List<GymTrainerDto> items, long total, int page, int pageSize) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public static class GymTrainersResponseBuilder {
        private List<GymTrainerDto> items;
        private long total;
        private int page;
        private int pageSize;

        GymTrainersResponseBuilder() {
        }

        public GymTrainersResponseBuilder items(List<GymTrainerDto> items) {
            this.items = items;
            return this;
        }

        public GymTrainersResponseBuilder total(long total) {
            this.total = total;
            return this;
        }

        public GymTrainersResponseBuilder page(int page) {
            this.page = page;
            return this;
        }

        public GymTrainersResponseBuilder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public GymTrainersResponse build() {
            return new GymTrainersResponse(this.items, this.total, this.page, this.pageSize);
        }

        public String toString() {
            return "GymTrainersResponse.GymTrainersResponseBuilder(items=" + this.items + ", total=" + this.total + ", page=" + this.page + ", pageSize=" + this.pageSize + ")";
        }
    }
}


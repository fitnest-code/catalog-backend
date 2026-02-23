/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.GymReviewDto;
import java.util.List;

public class GymReviewsResponse {
    private List<GymReviewDto> items;
    private long total;
    private int page;
    private int pageSize;

    public static GymReviewsResponseBuilder builder() {
        return new GymReviewsResponseBuilder();
    }

    public List<GymReviewDto> getItems() {
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

    public void setItems(List<GymReviewDto> items) {
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
        if (!(o instanceof GymReviewsResponse)) {
            return false;
        }
        GymReviewsResponse other = (GymReviewsResponse)o;
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
        List<GymReviewDto> this$items = this.getItems();
        List<GymReviewDto> other$items = other.getItems();
        return !(this$items == null ? other$items != null : !((Object)this$items).equals(other$items));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymReviewsResponse;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $total = this.getTotal();
        result = result * 59 + (int)($total >>> 32 ^ $total);
        result = result * 59 + this.getPage();
        result = result * 59 + this.getPageSize();
        List<GymReviewDto> $items = this.getItems();
        result = result * 59 + ($items == null ? 43 : ((Object)$items).hashCode());
        return result;
    }

    public String toString() {
        return "GymReviewsResponse(items=" + this.getItems() + ", total=" + this.getTotal() + ", page=" + this.getPage() + ", pageSize=" + this.getPageSize() + ")";
    }

    public GymReviewsResponse() {
    }

    public GymReviewsResponse(List<GymReviewDto> items, long total, int page, int pageSize) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public static class GymReviewsResponseBuilder {
        private List<GymReviewDto> items;
        private long total;
        private int page;
        private int pageSize;

        GymReviewsResponseBuilder() {
        }

        public GymReviewsResponseBuilder items(List<GymReviewDto> items) {
            this.items = items;
            return this;
        }

        public GymReviewsResponseBuilder total(long total) {
            this.total = total;
            return this;
        }

        public GymReviewsResponseBuilder page(int page) {
            this.page = page;
            return this;
        }

        public GymReviewsResponseBuilder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public GymReviewsResponse build() {
            return new GymReviewsResponse(this.items, this.total, this.page, this.pageSize);
        }

        public String toString() {
            return "GymReviewsResponse.GymReviewsResponseBuilder(items=" + this.items + ", total=" + this.total + ", page=" + this.page + ", pageSize=" + this.pageSize + ")";
        }
    }
}


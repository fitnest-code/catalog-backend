/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.StoreMainPageDto;
import java.util.List;

public class StoreResponseDto {
    private List<StoreMainPageDto> items;
    private long total;
    private int page;
    private int pageSize;

    public static StoreResponseDtoBuilder builder() {
        return new StoreResponseDtoBuilder();
    }

    public List<StoreMainPageDto> getItems() {
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

    public void setItems(List<StoreMainPageDto> items) {
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
        if (!(o instanceof StoreResponseDto)) {
            return false;
        }
        StoreResponseDto other = (StoreResponseDto)o;
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
        List<StoreMainPageDto> this$items = this.getItems();
        List<StoreMainPageDto> other$items = other.getItems();
        return !(this$items == null ? other$items != null : !((Object)this$items).equals(other$items));
    }

    protected boolean canEqual(Object other) {
        return other instanceof StoreResponseDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $total = this.getTotal();
        result = result * 59 + (int)($total >>> 32 ^ $total);
        result = result * 59 + this.getPage();
        result = result * 59 + this.getPageSize();
        List<StoreMainPageDto> $items = this.getItems();
        result = result * 59 + ($items == null ? 43 : ((Object)$items).hashCode());
        return result;
    }

    public String toString() {
        return "StoreResponseDto(items=" + this.getItems() + ", total=" + this.getTotal() + ", page=" + this.getPage() + ", pageSize=" + this.getPageSize() + ")";
    }

    public StoreResponseDto() {
    }

    public StoreResponseDto(List<StoreMainPageDto> items, long total, int page, int pageSize) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public static class StoreResponseDtoBuilder {
        private List<StoreMainPageDto> items;
        private long total;
        private int page;
        private int pageSize;

        StoreResponseDtoBuilder() {
        }

        public StoreResponseDtoBuilder items(List<StoreMainPageDto> items) {
            this.items = items;
            return this;
        }

        public StoreResponseDtoBuilder total(long total) {
            this.total = total;
            return this;
        }

        public StoreResponseDtoBuilder page(int page) {
            this.page = page;
            return this;
        }

        public StoreResponseDtoBuilder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public StoreResponseDto build() {
            return new StoreResponseDto(this.items, this.total, this.page, this.pageSize);
        }

        public String toString() {
            return "StoreResponseDto.StoreResponseDtoBuilder(items=" + this.items + ", total=" + this.total + ", page=" + this.page + ", pageSize=" + this.pageSize + ")";
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.CategoryDto;
import java.util.List;

public class CategoryResponseDto {
    private List<CategoryDto> items;
    private long total;
    private int page;
    private int pageSize;

    public static CategoryResponseDtoBuilder builder() {
        return new CategoryResponseDtoBuilder();
    }

    public List<CategoryDto> getItems() {
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

    public void setItems(List<CategoryDto> items) {
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
        if (!(o instanceof CategoryResponseDto)) {
            return false;
        }
        CategoryResponseDto other = (CategoryResponseDto)o;
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
        List<CategoryDto> this$items = this.getItems();
        List<CategoryDto> other$items = other.getItems();
        return !(this$items == null ? other$items != null : !((Object)this$items).equals(other$items));
    }

    protected boolean canEqual(Object other) {
        return other instanceof CategoryResponseDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $total = this.getTotal();
        result = result * 59 + (int)($total >>> 32 ^ $total);
        result = result * 59 + this.getPage();
        result = result * 59 + this.getPageSize();
        List<CategoryDto> $items = this.getItems();
        result = result * 59 + ($items == null ? 43 : ((Object)$items).hashCode());
        return result;
    }

    public String toString() {
        return "CategoryResponseDto(items=" + this.getItems() + ", total=" + this.getTotal() + ", page=" + this.getPage() + ", pageSize=" + this.getPageSize() + ")";
    }

    public CategoryResponseDto() {
    }

    public CategoryResponseDto(List<CategoryDto> items, long total, int page, int pageSize) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public static class CategoryResponseDtoBuilder {
        private List<CategoryDto> items;
        private long total;
        private int page;
        private int pageSize;

        CategoryResponseDtoBuilder() {
        }

        public CategoryResponseDtoBuilder items(List<CategoryDto> items) {
            this.items = items;
            return this;
        }

        public CategoryResponseDtoBuilder total(long total) {
            this.total = total;
            return this;
        }

        public CategoryResponseDtoBuilder page(int page) {
            this.page = page;
            return this;
        }

        public CategoryResponseDtoBuilder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public CategoryResponseDto build() {
            return new CategoryResponseDto(this.items, this.total, this.page, this.pageSize);
        }

        public String toString() {
            return "CategoryResponseDto.CategoryResponseDtoBuilder(items=" + this.items + ", total=" + this.total + ", page=" + this.page + ", pageSize=" + this.pageSize + ")";
        }
    }
}


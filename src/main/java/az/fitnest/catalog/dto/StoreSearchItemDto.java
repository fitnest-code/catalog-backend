/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

public class StoreSearchItemDto {
    private Long storeId;

    public static StoreSearchItemDtoBuilder builder() {
        return new StoreSearchItemDtoBuilder();
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StoreSearchItemDto)) {
            return false;
        }
        StoreSearchItemDto other = (StoreSearchItemDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        return !(this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId));
    }

    protected boolean canEqual(Object other) {
        return other instanceof StoreSearchItemDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
        return result;
    }

    public String toString() {
        return "StoreSearchItemDto(storeId=" + this.getStoreId() + ")";
    }

    public StoreSearchItemDto() {
    }

    public StoreSearchItemDto(Long storeId) {
        this.storeId = storeId;
    }

    public static class StoreSearchItemDtoBuilder {
        private Long storeId;

        StoreSearchItemDtoBuilder() {
        }

        public StoreSearchItemDtoBuilder storeId(Long storeId) {
            this.storeId = storeId;
            return this;
        }

        public StoreSearchItemDto build() {
            return new StoreSearchItemDto(this.storeId);
        }

        public String toString() {
            return "StoreSearchItemDto.StoreSearchItemDtoBuilder(storeId=" + this.storeId + ")";
        }
    }
}

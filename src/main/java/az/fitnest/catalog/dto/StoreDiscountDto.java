/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

public class StoreDiscountDto {
    private Integer percent;
    private String appliesTo;

    public static StoreDiscountDtoBuilder builder() {
        return new StoreDiscountDtoBuilder();
    }

    public Integer getPercent() {
        return this.percent;
    }

    public String getAppliesTo() {
        return this.appliesTo;
    }

    public void setPercent(Integer percent) {
        this.percent = percent;
    }

    public void setAppliesTo(String appliesTo) {
        this.appliesTo = appliesTo;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StoreDiscountDto)) {
            return false;
        }
        StoreDiscountDto other = (StoreDiscountDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$percent = this.getPercent();
        Integer other$percent = other.getPercent();
        if (this$percent == null ? other$percent != null : !((Object)this$percent).equals(other$percent)) {
            return false;
        }
        String this$appliesTo = this.getAppliesTo();
        String other$appliesTo = other.getAppliesTo();
        return !(this$appliesTo == null ? other$appliesTo != null : !this$appliesTo.equals(other$appliesTo));
    }

    protected boolean canEqual(Object other) {
        return other instanceof StoreDiscountDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $percent = this.getPercent();
        result = result * 59 + ($percent == null ? 43 : ((Object)$percent).hashCode());
        String $appliesTo = this.getAppliesTo();
        result = result * 59 + ($appliesTo == null ? 43 : $appliesTo.hashCode());
        return result;
    }

    public String toString() {
        return "StoreDiscountDto(percent=" + this.getPercent() + ", appliesTo=" + this.getAppliesTo() + ")";
    }

    public StoreDiscountDto() {
    }

    public StoreDiscountDto(Integer percent, String appliesTo) {
        this.percent = percent;
        this.appliesTo = appliesTo;
    }

    public static class StoreDiscountDtoBuilder {
        private Integer percent;
        private String appliesTo;

        StoreDiscountDtoBuilder() {
        }

        public StoreDiscountDtoBuilder percent(Integer percent) {
            this.percent = percent;
            return this;
        }

        public StoreDiscountDtoBuilder appliesTo(String appliesTo) {
            this.appliesTo = appliesTo;
            return this;
        }

        public StoreDiscountDto build() {
            return new StoreDiscountDto(this.percent, this.appliesTo);
        }

        public String toString() {
            return "StoreDiscountDto.StoreDiscountDtoBuilder(percent=" + this.percent + ", appliesTo=" + this.appliesTo + ")";
        }
    }
}


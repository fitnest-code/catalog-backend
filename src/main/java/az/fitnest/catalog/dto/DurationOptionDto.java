/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

public class DurationOptionDto {
    private Integer durationMonths;
    private BigDecimal priceStandard;
    private BigDecimal priceDiscounted;
    private Integer entryLimit;
    private Integer freezeDays;
    private List<String> services;

    public static DurationOptionDtoBuilder builder() {
        return new DurationOptionDtoBuilder();
    }

    public Integer getDurationMonths() {
        return this.durationMonths;
    }

    public BigDecimal getPriceStandard() {
        return this.priceStandard;
    }

    public BigDecimal getPriceDiscounted() {
        return this.priceDiscounted;
    }

    public Integer getEntryLimit() {
        return this.entryLimit;
    }

    public Integer getFreezeDays() {
        return this.freezeDays;
    }

    public List<String> getServices() {
        return this.services;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public void setPriceStandard(BigDecimal priceStandard) {
        this.priceStandard = priceStandard;
    }

    public void setPriceDiscounted(BigDecimal priceDiscounted) {
        this.priceDiscounted = priceDiscounted;
    }

    public void setEntryLimit(Integer entryLimit) {
        this.entryLimit = entryLimit;
    }

    public void setFreezeDays(Integer freezeDays) {
        this.freezeDays = freezeDays;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof DurationOptionDto)) {
            return false;
        }
        DurationOptionDto other = (DurationOptionDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$durationMonths = this.getDurationMonths();
        Integer other$durationMonths = other.getDurationMonths();
        if (this$durationMonths == null ? other$durationMonths != null : !((Object)this$durationMonths).equals(other$durationMonths)) {
            return false;
        }
        Integer this$entryLimit = this.getEntryLimit();
        Integer other$entryLimit = other.getEntryLimit();
        if (this$entryLimit == null ? other$entryLimit != null : !((Object)this$entryLimit).equals(other$entryLimit)) {
            return false;
        }
        Integer this$freezeDays = this.getFreezeDays();
        Integer other$freezeDays = other.getFreezeDays();
        if (this$freezeDays == null ? other$freezeDays != null : !((Object)this$freezeDays).equals(other$freezeDays)) {
            return false;
        }
        BigDecimal this$priceStandard = this.getPriceStandard();
        BigDecimal other$priceStandard = other.getPriceStandard();
        if (this$priceStandard == null ? other$priceStandard != null : !((Object)this$priceStandard).equals(other$priceStandard)) {
            return false;
        }
        BigDecimal this$priceDiscounted = this.getPriceDiscounted();
        BigDecimal other$priceDiscounted = other.getPriceDiscounted();
        if (this$priceDiscounted == null ? other$priceDiscounted != null : !((Object)this$priceDiscounted).equals(other$priceDiscounted)) {
            return false;
        }
        List<String> this$services = this.getServices();
        List<String> other$services = other.getServices();
        return !(this$services == null ? other$services != null : !((Object)this$services).equals(other$services));
    }

    protected boolean canEqual(Object other) {
        return other instanceof DurationOptionDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $durationMonths = this.getDurationMonths();
        result = result * 59 + ($durationMonths == null ? 43 : ((Object)$durationMonths).hashCode());
        Integer $entryLimit = this.getEntryLimit();
        result = result * 59 + ($entryLimit == null ? 43 : ((Object)$entryLimit).hashCode());
        Integer $freezeDays = this.getFreezeDays();
        result = result * 59 + ($freezeDays == null ? 43 : ((Object)$freezeDays).hashCode());
        BigDecimal $priceStandard = this.getPriceStandard();
        result = result * 59 + ($priceStandard == null ? 43 : ((Object)$priceStandard).hashCode());
        BigDecimal $priceDiscounted = this.getPriceDiscounted();
        result = result * 59 + ($priceDiscounted == null ? 43 : ((Object)$priceDiscounted).hashCode());
        List<String> $services = this.getServices();
        result = result * 59 + ($services == null ? 43 : ((Object)$services).hashCode());
        return result;
    }

    public String toString() {
        return "DurationOptionDto(durationMonths=" + this.getDurationMonths() + ", priceStandard=" + this.getPriceStandard() + ", priceDiscounted=" + this.getPriceDiscounted() + ", entryLimit=" + this.getEntryLimit() + ", freezeDays=" + this.getFreezeDays() + ", services=" + this.getServices() + ")";
    }

    public DurationOptionDto() {
    }

    public DurationOptionDto(Integer durationMonths, BigDecimal priceStandard, BigDecimal priceDiscounted, Integer entryLimit, Integer freezeDays, List<String> services) {
        this.durationMonths = durationMonths;
        this.priceStandard = priceStandard;
        this.priceDiscounted = priceDiscounted;
        this.entryLimit = entryLimit;
        this.freezeDays = freezeDays;
        this.services = services;
    }

    public static class DurationOptionDtoBuilder {
        private Integer durationMonths;
        private BigDecimal priceStandard;
        private BigDecimal priceDiscounted;
        private Integer entryLimit;
        private Integer freezeDays;
        private List<String> services;

        DurationOptionDtoBuilder() {
        }

        public DurationOptionDtoBuilder durationMonths(Integer durationMonths) {
            this.durationMonths = durationMonths;
            return this;
        }

        public DurationOptionDtoBuilder priceStandard(BigDecimal priceStandard) {
            this.priceStandard = priceStandard;
            return this;
        }

        public DurationOptionDtoBuilder priceDiscounted(BigDecimal priceDiscounted) {
            this.priceDiscounted = priceDiscounted;
            return this;
        }

        public DurationOptionDtoBuilder entryLimit(Integer entryLimit) {
            this.entryLimit = entryLimit;
            return this;
        }

        public DurationOptionDtoBuilder freezeDays(Integer freezeDays) {
            this.freezeDays = freezeDays;
            return this;
        }

        public DurationOptionDtoBuilder services(List<String> services) {
            this.services = services;
            return this;
        }

        public DurationOptionDto build() {
            return new DurationOptionDto(this.durationMonths, this.priceStandard, this.priceDiscounted, this.entryLimit, this.freezeDays, this.services);
        }

        public String toString() {
            return "DurationOptionDto.DurationOptionDtoBuilder(durationMonths=" + this.durationMonths + ", priceStandard=" + this.priceStandard + ", priceDiscounted=" + this.priceDiscounted + ", entryLimit=" + this.entryLimit + ", freezeDays=" + this.freezeDays + ", services=" + this.services + ")";
        }
    }
}


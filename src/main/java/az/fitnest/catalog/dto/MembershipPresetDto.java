/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.DurationOptionDto;
import java.util.List;

public class MembershipPresetDto {
    private String name;
    private List<DurationOptionDto> options;

    public static MembershipPresetDtoBuilder builder() {
        return new MembershipPresetDtoBuilder();
    }

    public String getName() {
        return this.name;
    }

    public List<DurationOptionDto> getOptions() {
        return this.options;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOptions(List<DurationOptionDto> options) {
        this.options = options;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MembershipPresetDto)) {
            return false;
        }
        MembershipPresetDto other = (MembershipPresetDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        List<DurationOptionDto> this$options = this.getOptions();
        List<DurationOptionDto> other$options = other.getOptions();
        return !(this$options == null ? other$options != null : !((Object)this$options).equals(other$options));
    }

    protected boolean canEqual(Object other) {
        return other instanceof MembershipPresetDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        List<DurationOptionDto> $options = this.getOptions();
        result = result * 59 + ($options == null ? 43 : ((Object)$options).hashCode());
        return result;
    }

    public String toString() {
        return "MembershipPresetDto(name=" + this.getName() + ", options=" + this.getOptions() + ")";
    }

    public MembershipPresetDto() {
    }

    public MembershipPresetDto(String name, List<DurationOptionDto> options) {
        this.name = name;
        this.options = options;
    }

    public static class MembershipPresetDtoBuilder {
        private String name;
        private List<DurationOptionDto> options;

        MembershipPresetDtoBuilder() {
        }

        public MembershipPresetDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public MembershipPresetDtoBuilder options(List<DurationOptionDto> options) {
            this.options = options;
            return this;
        }

        public MembershipPresetDto build() {
            return new MembershipPresetDto(this.name, this.options);
        }

        public String toString() {
            return "MembershipPresetDto.MembershipPresetDtoBuilder(name=" + this.name + ", options=" + this.options + ")";
        }
    }
}


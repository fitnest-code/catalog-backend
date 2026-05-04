package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import java.util.List;

public class FilterResponse {
    private List<TabDto> tabs;
    private List<String> sortOptions;
    private int defaultRadiusKm;

    public FilterResponse() {
    }

    public FilterResponse(List<TabDto> tabs, List<String> sortOptions, int defaultRadiusKm) {
        this.tabs = tabs;
        this.sortOptions = sortOptions;
        this.defaultRadiusKm = defaultRadiusKm;
    }

    public static FilterResponseDtoBuilder builder() {
        return new FilterResponseDtoBuilder();
    }

    public List<TabDto> getTabs() {
        return this.tabs;
    }

    public void setTabs(List<TabDto> tabs) {
        this.tabs = tabs;
    }

    public List<String> getSortOptions() {
        return this.sortOptions;
    }

    public void setSortOptions(List<String> sortOptions) {
        this.sortOptions = sortOptions;
    }

    public int getDefaultRadiusKm() {
        return this.defaultRadiusKm;
    }

    public void setDefaultRadiusKm(int defaultRadiusKm) {
        this.defaultRadiusKm = defaultRadiusKm;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof FilterResponse)) {
            return false;
        }
        FilterResponse other = (FilterResponse) o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getDefaultRadiusKm() != other.getDefaultRadiusKm()) {
            return false;
        }
        List<TabDto> this$tabs = this.getTabs();
        List<TabDto> other$tabs = other.getTabs();
        if (this$tabs == null ? other$tabs != null : !((Object) this$tabs).equals(other$tabs)) {
            return false;
        }
        List<String> this$sortOptions = this.getSortOptions();
        List<String> other$sortOptions = other.getSortOptions();
        return !(this$sortOptions == null ? other$sortOptions != null : !((Object) this$sortOptions).equals(other$sortOptions));
    }

    protected boolean canEqual(Object other) {
        return other instanceof FilterResponse;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getDefaultRadiusKm();
        List<TabDto> $tabs = this.getTabs();
        result = result * 59 + ($tabs == null ? 43 : ((Object) $tabs).hashCode());
        List<String> $sortOptions = this.getSortOptions();
        result = result * 59 + ($sortOptions == null ? 43 : ((Object) $sortOptions).hashCode());
        return result;
    }

    public String toString() {
        return "FilterResponse(tabs=" + this.getTabs() + ", sortOptions=" + this.getSortOptions() + ", defaultRadiusKm=" + this.getDefaultRadiusKm() + ")";
    }

    public static class FilterResponseDtoBuilder {
        private List<TabDto> tabs;
        private List<String> sortOptions;
        private int defaultRadiusKm;

        FilterResponseDtoBuilder() {
        }

        public FilterResponseDtoBuilder tabs(List<TabDto> tabs) {
            this.tabs = tabs;
            return this;
        }

        public FilterResponseDtoBuilder sortOptions(List<String> sortOptions) {
            this.sortOptions = sortOptions;
            return this;
        }

        public FilterResponseDtoBuilder defaultRadiusKm(int defaultRadiusKm) {
            this.defaultRadiusKm = defaultRadiusKm;
            return this;
        }

        public FilterResponse build() {
            return new FilterResponse(this.tabs, this.sortOptions, this.defaultRadiusKm);
        }

        public String toString() {
            return "FilterResponse.FilterResponseDtoBuilder(tabs=" + this.tabs + ", sortOptions=" + this.sortOptions + ", defaultRadiusKm=" + this.defaultRadiusKm + ")";
        }
    }

    public static class TabDto {
        private String code;
        private String title;

        public TabDto() {
        }

        public TabDto(String code, String title) {
            this.code = code;
            this.title = title;
        }

        public static TabDtoBuilder builder() {
            return new TabDtoBuilder();
        }

        public String getCode() {
            return this.code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getTitle() {
            return this.title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof TabDto)) {
                return false;
            }
            TabDto other = (TabDto) o;
            if (!other.canEqual(this)) {
                return false;
            }
            String this$code = this.getCode();
            String other$code = other.getCode();
            if (this$code == null ? other$code != null : !this$code.equals(other$code)) {
                return false;
            }
            String this$title = this.getTitle();
            String other$title = other.getTitle();
            return !(this$title == null ? other$title != null : !this$title.equals(other$title));
        }

        protected boolean canEqual(Object other) {
            return other instanceof TabDto;
        }

        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            String $code = this.getCode();
            result = result * 59 + ($code == null ? 43 : $code.hashCode());
            String $title = this.getTitle();
            result = result * 59 + ($title == null ? 43 : $title.hashCode());
            return result;
        }

        public String toString() {
            return "FilterResponse.TabDto(code=" + this.getCode() + ", title=" + this.getTitle() + ")";
        }

        public static class TabDtoBuilder {
            private String code;
            private String title;

            TabDtoBuilder() {
            }

            public TabDtoBuilder code(String code) {
                this.code = code;
                return this;
            }

            public TabDtoBuilder title(String title) {
                this.title = title;
                return this;
            }

            public TabDto build() {
                return new TabDto(this.code, this.title);
            }

            public String toString() {
                return "FilterResponse.TabDto.TabDtoBuilder(code=" + this.code + ", title=" + this.title + ")";
            }
        }
    }
}

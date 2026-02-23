/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

public class GymSocialLinkDto {
    private String name;
    private String url;

    public static GymSocialLinkDtoBuilder builder() {
        return new GymSocialLinkDtoBuilder();
    }

    public String getName() {
        return this.name;
    }

    public String getUrl() {
        return this.url;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymSocialLinkDto)) {
            return false;
        }
        GymSocialLinkDto other = (GymSocialLinkDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$url = this.getUrl();
        String other$url = other.getUrl();
        return !(this$url == null ? other$url != null : !this$url.equals(other$url));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymSocialLinkDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $url = this.getUrl();
        result = result * 59 + ($url == null ? 43 : $url.hashCode());
        return result;
    }

    public String toString() {
        return "GymSocialLinkDto(name=" + this.getName() + ", url=" + this.getUrl() + ")";
    }

    public GymSocialLinkDto() {
    }

    public GymSocialLinkDto(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public static class GymSocialLinkDtoBuilder {
        private String name;
        private String url;

        GymSocialLinkDtoBuilder() {
        }

        public GymSocialLinkDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public GymSocialLinkDtoBuilder url(String url) {
            this.url = url;
            return this;
        }

        public GymSocialLinkDto build() {
            return new GymSocialLinkDto(this.name, this.url);
        }

        public String toString() {
            return "GymSocialLinkDto.GymSocialLinkDtoBuilder(name=" + this.name + ", url=" + this.url + ")";
        }
    }
}


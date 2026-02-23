/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

public class StoreSocialDto {
    private String instagramUrl;
    private String facebookUrl;
    private String websiteUrl;

    public static StoreSocialDtoBuilder builder() {
        return new StoreSocialDtoBuilder();
    }

    public String getInstagramUrl() {
        return this.instagramUrl;
    }

    public String getFacebookUrl() {
        return this.facebookUrl;
    }

    public String getWebsiteUrl() {
        return this.websiteUrl;
    }

    public void setInstagramUrl(String instagramUrl) {
        this.instagramUrl = instagramUrl;
    }

    public void setFacebookUrl(String facebookUrl) {
        this.facebookUrl = facebookUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StoreSocialDto)) {
            return false;
        }
        StoreSocialDto other = (StoreSocialDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$instagramUrl = this.getInstagramUrl();
        String other$instagramUrl = other.getInstagramUrl();
        if (this$instagramUrl == null ? other$instagramUrl != null : !this$instagramUrl.equals(other$instagramUrl)) {
            return false;
        }
        String this$facebookUrl = this.getFacebookUrl();
        String other$facebookUrl = other.getFacebookUrl();
        if (this$facebookUrl == null ? other$facebookUrl != null : !this$facebookUrl.equals(other$facebookUrl)) {
            return false;
        }
        String this$websiteUrl = this.getWebsiteUrl();
        String other$websiteUrl = other.getWebsiteUrl();
        return !(this$websiteUrl == null ? other$websiteUrl != null : !this$websiteUrl.equals(other$websiteUrl));
    }

    protected boolean canEqual(Object other) {
        return other instanceof StoreSocialDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $instagramUrl = this.getInstagramUrl();
        result = result * 59 + ($instagramUrl == null ? 43 : $instagramUrl.hashCode());
        String $facebookUrl = this.getFacebookUrl();
        result = result * 59 + ($facebookUrl == null ? 43 : $facebookUrl.hashCode());
        String $websiteUrl = this.getWebsiteUrl();
        result = result * 59 + ($websiteUrl == null ? 43 : $websiteUrl.hashCode());
        return result;
    }

    public String toString() {
        return "StoreSocialDto(instagramUrl=" + this.getInstagramUrl() + ", facebookUrl=" + this.getFacebookUrl() + ", websiteUrl=" + this.getWebsiteUrl() + ")";
    }

    public StoreSocialDto() {
    }

    public StoreSocialDto(String instagramUrl, String facebookUrl, String websiteUrl) {
        this.instagramUrl = instagramUrl;
        this.facebookUrl = facebookUrl;
        this.websiteUrl = websiteUrl;
    }

    public static class StoreSocialDtoBuilder {
        private String instagramUrl;
        private String facebookUrl;
        private String websiteUrl;

        StoreSocialDtoBuilder() {
        }

        public StoreSocialDtoBuilder instagramUrl(String instagramUrl) {
            this.instagramUrl = instagramUrl;
            return this;
        }

        public StoreSocialDtoBuilder facebookUrl(String facebookUrl) {
            this.facebookUrl = facebookUrl;
            return this;
        }

        public StoreSocialDtoBuilder websiteUrl(String websiteUrl) {
            this.websiteUrl = websiteUrl;
            return this;
        }

        public StoreSocialDto build() {
            return new StoreSocialDto(this.instagramUrl, this.facebookUrl, this.websiteUrl);
        }

        public String toString() {
            return "StoreSocialDto.StoreSocialDtoBuilder(instagramUrl=" + this.instagramUrl + ", facebookUrl=" + this.facebookUrl + ", websiteUrl=" + this.websiteUrl + ")";
        }
    }
}


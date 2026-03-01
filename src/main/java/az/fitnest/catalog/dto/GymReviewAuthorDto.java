/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

public class GymReviewAuthorDto {
    private String user_id;
    private String full_name;
    private String avatar_url;

    public GymReviewAuthorDto() {
    }

    public GymReviewAuthorDto(String user_id, String full_name, String avatar_url) {
        this.user_id = user_id;
        this.full_name = full_name;
        this.avatar_url = avatar_url;
    }

    public static GymReviewAuthorDtoBuilder builder() {
        return new GymReviewAuthorDtoBuilder();
    }

    public String getUser_id() {
        return this.user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getFull_name() {
        return this.full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public String getAvatar_url() {
        return this.avatar_url;
    }

    public void setAvatar_url(String avatar_url) {
        this.avatar_url = avatar_url;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymReviewAuthorDto)) {
            return false;
        }
        GymReviewAuthorDto other = (GymReviewAuthorDto) o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$user_id = this.getUser_id();
        String other$user_id = other.getUser_id();
        if (this$user_id == null ? other$user_id != null : !this$user_id.equals(other$user_id)) {
            return false;
        }
        String this$full_name = this.getFull_name();
        String other$full_name = other.getFull_name();
        if (this$full_name == null ? other$full_name != null : !this$full_name.equals(other$full_name)) {
            return false;
        }
        String this$avatar_url = this.getAvatar_url();
        String other$avatar_url = other.getAvatar_url();
        return !(this$avatar_url == null ? other$avatar_url != null : !this$avatar_url.equals(other$avatar_url));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymReviewAuthorDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $user_id = this.getUser_id();
        result = result * 59 + ($user_id == null ? 43 : $user_id.hashCode());
        String $full_name = this.getFull_name();
        result = result * 59 + ($full_name == null ? 43 : $full_name.hashCode());
        String $avatar_url = this.getAvatar_url();
        result = result * 59 + ($avatar_url == null ? 43 : $avatar_url.hashCode());
        return result;
    }

    public String toString() {
        return "GymReviewAuthorDto(user_id=" + this.getUser_id() + ", full_name=" + this.getFull_name() + ", avatar_url=" + this.getAvatar_url() + ")";
    }

    public static class GymReviewAuthorDtoBuilder {
        private String user_id;
        private String full_name;
        private String avatar_url;

        GymReviewAuthorDtoBuilder() {
        }

        public GymReviewAuthorDtoBuilder user_id(String user_id) {
            this.user_id = user_id;
            return this;
        }

        public GymReviewAuthorDtoBuilder full_name(String full_name) {
            this.full_name = full_name;
            return this;
        }

        public GymReviewAuthorDtoBuilder avatar_url(String avatar_url) {
            this.avatar_url = avatar_url;
            return this;
        }

        public GymReviewAuthorDto build() {
            return new GymReviewAuthorDto(this.user_id, this.full_name, this.avatar_url);
        }

        public String toString() {
            return "GymReviewAuthorDto.GymReviewAuthorDtoBuilder(user_id=" + this.user_id + ", full_name=" + this.full_name + ", avatar_url=" + this.avatar_url + ")";
        }
    }
}


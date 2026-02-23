/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

public class GymImageItemDto {
    private String image_id;
    private String type;
    private String title;
    private String url;

    public static GymImageItemDtoBuilder builder() {
        return new GymImageItemDtoBuilder();
    }

    public String getImage_id() {
        return this.image_id;
    }

    public String getType() {
        return this.type;
    }

    public String getTitle() {
        return this.title;
    }

    public String getUrl() {
        return this.url;
    }

    public void setImage_id(String image_id) {
        this.image_id = image_id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymImageItemDto)) {
            return false;
        }
        GymImageItemDto other = (GymImageItemDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$image_id = this.getImage_id();
        String other$image_id = other.getImage_id();
        if (this$image_id == null ? other$image_id != null : !this$image_id.equals(other$image_id)) {
            return false;
        }
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
            return false;
        }
        String this$url = this.getUrl();
        String other$url = other.getUrl();
        return !(this$url == null ? other$url != null : !this$url.equals(other$url));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymImageItemDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $image_id = this.getImage_id();
        result = result * 59 + ($image_id == null ? 43 : $image_id.hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $url = this.getUrl();
        result = result * 59 + ($url == null ? 43 : $url.hashCode());
        return result;
    }

    public String toString() {
        return "GymImageItemDto(image_id=" + this.getImage_id() + ", type=" + this.getType() + ", title=" + this.getTitle() + ", url=" + this.getUrl() + ")";
    }

    public GymImageItemDto() {
    }

    public GymImageItemDto(String image_id, String type, String title, String url) {
        this.image_id = image_id;
        this.type = type;
        this.title = title;
        this.url = url;
    }

    public static class GymImageItemDtoBuilder {
        private String image_id;
        private String type;
        private String title;
        private String url;

        GymImageItemDtoBuilder() {
        }

        public GymImageItemDtoBuilder image_id(String image_id) {
            this.image_id = image_id;
            return this;
        }

        public GymImageItemDtoBuilder type(String type) {
            this.type = type;
            return this;
        }

        public GymImageItemDtoBuilder title(String title) {
            this.title = title;
            return this;
        }

        public GymImageItemDtoBuilder url(String url) {
            this.url = url;
            return this;
        }

        public GymImageItemDto build() {
            return new GymImageItemDto(this.image_id, this.type, this.title, this.url);
        }

        public String toString() {
            return "GymImageItemDto.GymImageItemDtoBuilder(image_id=" + this.image_id + ", type=" + this.type + ", title=" + this.title + ", url=" + this.url + ")";
        }
    }
}


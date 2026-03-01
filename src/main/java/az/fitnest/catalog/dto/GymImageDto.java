/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

public class GymImageDto {
    private Long id;
    private Long gymId;
    private String name;
    private String url;

    public GymImageDto() {
    }

    public GymImageDto(Long id, Long gymId, String name, String url) {
        this.id = id;
        this.gymId = gymId;
        this.name = name;
        this.url = url;
    }

    public static GymImageDtoBuilder builder() {
        return new GymImageDtoBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGymId() {
        return this.gymId;
    }

    public void setGymId(Long gymId) {
        this.gymId = gymId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymImageDto)) {
            return false;
        }
        GymImageDto other = (GymImageDto) o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object) this$id).equals(other$id)) {
            return false;
        }
        Long this$gymId = this.getGymId();
        Long other$gymId = other.getGymId();
        if (this$gymId == null ? other$gymId != null : !((Object) this$gymId).equals(other$gymId)) {
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
        return other instanceof GymImageDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object) $id).hashCode());
        Long $gymId = this.getGymId();
        result = result * 59 + ($gymId == null ? 43 : ((Object) $gymId).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $url = this.getUrl();
        result = result * 59 + ($url == null ? 43 : $url.hashCode());
        return result;
    }

    public String toString() {
        return "GymImageDto(id=" + this.getId() + ", gymId=" + this.getGymId() + ", name=" + this.getName() + ", url=" + this.getUrl() + ")";
    }

    public static class GymImageDtoBuilder {
        private Long id;
        private Long gymId;
        private String name;
        private String url;

        GymImageDtoBuilder() {
        }

        public GymImageDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public GymImageDtoBuilder gymId(Long gymId) {
            this.gymId = gymId;
            return this;
        }

        public GymImageDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public GymImageDtoBuilder url(String url) {
            this.url = url;
            return this;
        }

        public GymImageDto build() {
            return new GymImageDto(this.id, this.gymId, this.name, this.url);
        }

        public String toString() {
            return "GymImageDto.GymImageDtoBuilder(id=" + this.id + ", gymId=" + this.gymId + ", name=" + this.name + ", url=" + this.url + ")";
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

public class CategoryDto {
    private Long id;
    private String name;
    private String photoUrl;

    public static CategoryDtoBuilder builder() {
        return new CategoryDtoBuilder();
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getPhotoUrl() {
        return this.photoUrl;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CategoryDto)) {
            return false;
        }
        CategoryDto other = (CategoryDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$photoUrl = this.getPhotoUrl();
        String other$photoUrl = other.getPhotoUrl();
        return !(this$photoUrl == null ? other$photoUrl != null : !this$photoUrl.equals(other$photoUrl));
    }

    protected boolean canEqual(Object other) {
        return other instanceof CategoryDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $photoUrl = this.getPhotoUrl();
        result = result * 59 + ($photoUrl == null ? 43 : $photoUrl.hashCode());
        return result;
    }

    public String toString() {
        return "CategoryDto(id=" + this.getId() + ", name=" + this.getName() + ", photoUrl=" + this.getPhotoUrl() + ")";
    }

    public CategoryDto() {
    }

    public CategoryDto(Long id, String name, String photoUrl) {
        this.id = id;
        this.name = name;
        this.photoUrl = photoUrl;
    }

    public static class CategoryDtoBuilder {
        private Long id;
        private String name;
        private String photoUrl;

        CategoryDtoBuilder() {
        }

        public CategoryDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public CategoryDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CategoryDtoBuilder photoUrl(String photoUrl) {
            this.photoUrl = photoUrl;
            return this;
        }

        public CategoryDto build() {
            return new CategoryDto(this.id, this.name, this.photoUrl);
        }

        public String toString() {
            return "CategoryDto.CategoryDtoBuilder(id=" + this.id + ", name=" + this.name + ", photoUrl=" + this.photoUrl + ")";
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.GymImageDto;
import java.util.List;

public class GymRoomDto {
    private List<GymImageDto> images;

    public static GymRoomDtoBuilder builder() {
        return new GymRoomDtoBuilder();
    }

    public List<GymImageDto> getImages() {
        return this.images;
    }

    public void setImages(List<GymImageDto> images) {
        this.images = images;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymRoomDto)) {
            return false;
        }
        GymRoomDto other = (GymRoomDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        List<GymImageDto> this$images = this.getImages();
        List<GymImageDto> other$images = other.getImages();
        return !(this$images == null ? other$images != null : !((Object)this$images).equals(other$images));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymRoomDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        List<GymImageDto> $images = this.getImages();
        result = result * 59 + ($images == null ? 43 : ((Object)$images).hashCode());
        return result;
    }

    public String toString() {
        return "GymRoomDto(images=" + this.getImages() + ")";
    }

    public GymRoomDto() {
    }

    public GymRoomDto(List<GymImageDto> images) {
        this.images = images;
    }

    public static class GymRoomDtoBuilder {
        private List<GymImageDto> images;

        GymRoomDtoBuilder() {
        }

        public GymRoomDtoBuilder images(List<GymImageDto> images) {
            this.images = images;
            return this;
        }

        public GymRoomDto build() {
            return new GymRoomDto(this.images);
        }

        public String toString() {
            return "GymRoomDto.GymRoomDtoBuilder(images=" + this.images + ")";
        }
    }
}


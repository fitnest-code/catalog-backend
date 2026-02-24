/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.GymImageDto;
import java.util.List;

public class GymRoomDto {
    private String room_name;
    private List<GymImageDto> images;

    public String getRoom_name() {
        return this.room_name;
    }

    public void setRoom_name(String room_name) {
        this.room_name = room_name;
    }

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
        if (this$images == null ? other$images != null : !((Object)this$images).equals(other$images)) {
            return false;
        }
        String this$room_name = this.getRoom_name();
        String other$room_name = other.getRoom_name();
        return !(this$room_name == null ? other$room_name != null : !this$room_name.equals(other$room_name));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymRoomDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        List<GymImageDto> $images = this.getImages();
        result = result * 59 + ($images == null ? 43 : ((Object)$images).hashCode());
        String $room_name = this.getRoom_name();
        result = result * 59 + ($room_name == null ? 43 : $room_name.hashCode());
        return result;
    }

    public String toString() {
        return "GymRoomDto(room_name=" + this.getRoom_name() + ", images=" + this.getImages() + ")";
    }

    public GymRoomDto() {
    }

    public GymRoomDto(String room_name, List<GymImageDto> images) {
        this.room_name = room_name;
        this.images = images;
    }

    public static class GymRoomDtoBuilder {
        private String room_name;
        private List<GymImageDto> images;

        GymRoomDtoBuilder() {
        }

        public GymRoomDtoBuilder images(List<GymImageDto> images) {
            this.images = images;
            return this;
        }

        public GymRoomDtoBuilder room_name(String room_name) {
            this.room_name = room_name;
            return this;
        }

        public GymRoomDto build() {
            return new GymRoomDto(this.room_name, this.images);
        }

        public String toString() {
            return "GymRoomDto.GymRoomDtoBuilder(room_name=" + this.room_name + ", images=" + this.images + ")";
        }
    }
}


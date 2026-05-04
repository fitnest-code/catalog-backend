package az.fitnest.catalog.dto.response;
import az.fitnest.catalog.dto.request.*;
import az.fitnest.catalog.dto.*;

import com.fasterxml.jackson.annotation.JsonInclude;

public class GymNearbyResponse {
    private Long gymId;
    private String name;
    private String address;
    private Double rating;
    private Boolean isNew;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Double distanceKm;

    public GymNearbyResponse() {
    }

    public GymNearbyResponse(Long gymId, String name, String address, Double rating, Boolean isNew, Double distanceKm) {
        this.gymId = gymId;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.isNew = isNew;
        this.distanceKm = distanceKm;
    }

    public static GymNearbyResponseDtoBuilder builder() {
        return new GymNearbyResponseDtoBuilder();
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

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getRating() {
        return this.rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Boolean getIsNew() {
        return this.isNew;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }

    public Double getDistanceKm() {
        return this.distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymNearbyResponse)) {
            return false;
        }
        GymNearbyResponse other = (GymNearbyResponse) o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$gymId = this.getGymId();
        Long other$gymId = other.getGymId();
        if (this$gymId == null ? other$gymId != null : !((Object) this$gymId).equals(other$gymId)) {
            return false;
        }
        Double this$rating = this.getRating();
        Double other$rating = other.getRating();
        if (this$rating == null ? other$rating != null : !((Object) this$rating).equals(other$rating)) {
            return false;
        }
        Boolean this$isNew = this.getIsNew();
        Boolean other$isNew = other.getIsNew();
        if (this$isNew == null ? other$isNew != null : !((Object) this$isNew).equals(other$isNew)) {
            return false;
        }
        Double this$distanceKm = this.getDistanceKm();
        Double other$distanceKm = other.getDistanceKm();
        if (this$distanceKm == null ? other$distanceKm != null : !((Object) this$distanceKm).equals(other$distanceKm)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$address = this.getAddress();
        String other$address = other.getAddress();
        return !(this$address == null ? other$address != null : !this$address.equals(other$address));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymNearbyResponse;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $gymId = this.getGymId();
        result = result * 59 + ($gymId == null ? 43 : ((Object) $gymId).hashCode());
        Double $rating = this.getRating();
        result = result * 59 + ($rating == null ? 43 : ((Object) $rating).hashCode());
        Boolean $isNew = this.getIsNew();
        result = result * 59 + ($isNew == null ? 43 : ((Object) $isNew).hashCode());
        Double $distanceKm = this.getDistanceKm();
        result = result * 59 + ($distanceKm == null ? 43 : ((Object) $distanceKm).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $address = this.getAddress();
        result = result * 59 + ($address == null ? 43 : $address.hashCode());
        return result;
    }

    public String toString() {
        return "GymNearbyResponse(gymId=" + this.getGymId() + ", name=" + this.getName() + ", address=" + this.getAddress() + ", rating=" + this.getRating() + ", isNew=" + this.getIsNew() + ", distanceKm=" + this.getDistanceKm() + ")";
    }

    public static class GymNearbyResponseDtoBuilder {
        private Long gymId;
        private String name;
        private String address;
        private Double rating;
        private Boolean isNew;
        private Double distanceKm;

        GymNearbyResponseDtoBuilder() {
        }

        public GymNearbyResponseDtoBuilder gymId(Long gymId) {
            this.gymId = gymId;
            return this;
        }

        public GymNearbyResponseDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public GymNearbyResponseDtoBuilder address(String address) {
            this.address = address;
            return this;
        }

        public GymNearbyResponseDtoBuilder rating(Double rating) {
            this.rating = rating;
            return this;
        }

        public GymNearbyResponseDtoBuilder isNew(Boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        public GymNearbyResponseDtoBuilder distanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        public GymNearbyResponse build() {
            return new GymNearbyResponse(this.gymId, this.name, this.address, this.rating, this.isNew, this.distanceKm);
        }

        public String toString() {
            return "GymNearbyResponse.GymNearbyResponseDtoBuilder(gymId=" + this.gymId + ", name=" + this.name + ", address=" + this.address + ", rating=" + this.rating + ", isNew=" + this.isNew + ", distanceKm=" + this.distanceKm + ")";
        }
    }
}

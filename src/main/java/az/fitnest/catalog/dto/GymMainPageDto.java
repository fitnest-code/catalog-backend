/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

public class GymMainPageDto {
    private String gymId;
    private String name;
    private String imageUrl;
    private double stars;
    private boolean isNew;
    private String location;
    private Double distanceKm;

    public static GymMainPageDtoBuilder builder() {
        return new GymMainPageDtoBuilder();
    }

    public String getGymId() {
        return this.gymId;
    }

    public String getName() {
        return this.name;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public double getStars() {
        return this.stars;
    }

    public boolean isNew() {
        return this.isNew;
    }

    public String getLocation() {
        return this.location;
    }

    public Double getDistanceKm() {
        return this.distanceKm;
    }

    public void setGymId(String gymId) {
        this.gymId = gymId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setStars(double stars) {
        this.stars = stars;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymMainPageDto)) {
            return false;
        }
        GymMainPageDto other = (GymMainPageDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (Double.compare(this.getStars(), other.getStars()) != 0) {
            return false;
        }
        if (this.isNew() != other.isNew()) {
            return false;
        }
        Double this$distanceKm = this.getDistanceKm();
        Double other$distanceKm = other.getDistanceKm();
        if (this$distanceKm == null ? other$distanceKm != null : !((Object)this$distanceKm).equals(other$distanceKm)) {
            return false;
        }
        String this$gymId = this.getGymId();
        String other$gymId = other.getGymId();
        if (this$gymId == null ? other$gymId != null : !this$gymId.equals(other$gymId)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$imageUrl = this.getImageUrl();
        String other$imageUrl = other.getImageUrl();
        if (this$imageUrl == null ? other$imageUrl != null : !this$imageUrl.equals(other$imageUrl)) {
            return false;
        }
        String this$location = this.getLocation();
        String other$location = other.getLocation();
        return !(this$location == null ? other$location != null : !this$location.equals(other$location));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymMainPageDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $stars = Double.doubleToLongBits(this.getStars());
        result = result * 59 + (int)($stars >>> 32 ^ $stars);
        result = result * 59 + (this.isNew() ? 79 : 97);
        Double $distanceKm = this.getDistanceKm();
        result = result * 59 + ($distanceKm == null ? 43 : ((Object)$distanceKm).hashCode());
        String $gymId = this.getGymId();
        result = result * 59 + ($gymId == null ? 43 : $gymId.hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $imageUrl = this.getImageUrl();
        result = result * 59 + ($imageUrl == null ? 43 : $imageUrl.hashCode());
        String $location = this.getLocation();
        result = result * 59 + ($location == null ? 43 : $location.hashCode());
        return result;
    }

    public String toString() {
        return "GymMainPageDto(gymId=" + this.getGymId() + ", name=" + this.getName() + ", imageUrl=" + this.getImageUrl() + ", stars=" + this.getStars() + ", isNew=" + this.isNew() + ", location=" + this.getLocation() + ", distanceKm=" + this.getDistanceKm() + ")";
    }

    public GymMainPageDto() {
    }

    public GymMainPageDto(String gymId, String name, String imageUrl, double stars, boolean isNew, String location, Double distanceKm) {
        this.gymId = gymId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.stars = stars;
        this.isNew = isNew;
        this.location = location;
        this.distanceKm = distanceKm;
    }

    public static class GymMainPageDtoBuilder {
        private String gymId;
        private String name;
        private String imageUrl;
        private double stars;
        private boolean isNew;
        private String location;
        private Double distanceKm;

        GymMainPageDtoBuilder() {
        }

        public GymMainPageDtoBuilder gymId(String gymId) {
            this.gymId = gymId;
            return this;
        }

        public GymMainPageDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public GymMainPageDtoBuilder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public GymMainPageDtoBuilder stars(double stars) {
            this.stars = stars;
            return this;
        }

        public GymMainPageDtoBuilder isNew(boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        public GymMainPageDtoBuilder location(String location) {
            this.location = location;
            return this;
        }

        public GymMainPageDtoBuilder distanceKm(Double distanceKm) {
            this.distanceKm = distanceKm;
            return this;
        }

        public GymMainPageDto build() {
            return new GymMainPageDto(this.gymId, this.name, this.imageUrl, this.stars, this.isNew, this.location, this.distanceKm);
        }

        public String toString() {
            return "GymMainPageDto.GymMainPageDtoBuilder(gymId=" + this.gymId + ", name=" + this.name + ", imageUrl=" + this.imageUrl + ", stars=" + this.stars + ", isNew=" + this.isNew + ", location=" + this.location + ", distanceKm=" + this.distanceKm + ")";
        }
    }
}


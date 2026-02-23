/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

public class GymTrainerDto {
    private String trainer_id;
    private String full_name;
    private String specialization;
    private String image_url;

    public static GymTrainerDtoBuilder builder() {
        return new GymTrainerDtoBuilder();
    }

    public String getTrainer_id() {
        return this.trainer_id;
    }

    public String getFull_name() {
        return this.full_name;
    }

    public String getSpecialization() {
        return this.specialization;
    }

    public String getImage_url() {
        return this.image_url;
    }

    public void setTrainer_id(String trainer_id) {
        this.trainer_id = trainer_id;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymTrainerDto)) {
            return false;
        }
        GymTrainerDto other = (GymTrainerDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$trainer_id = this.getTrainer_id();
        String other$trainer_id = other.getTrainer_id();
        if (this$trainer_id == null ? other$trainer_id != null : !this$trainer_id.equals(other$trainer_id)) {
            return false;
        }
        String this$full_name = this.getFull_name();
        String other$full_name = other.getFull_name();
        if (this$full_name == null ? other$full_name != null : !this$full_name.equals(other$full_name)) {
            return false;
        }
        String this$specialization = this.getSpecialization();
        String other$specialization = other.getSpecialization();
        if (this$specialization == null ? other$specialization != null : !this$specialization.equals(other$specialization)) {
            return false;
        }
        String this$image_url = this.getImage_url();
        String other$image_url = other.getImage_url();
        return !(this$image_url == null ? other$image_url != null : !this$image_url.equals(other$image_url));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymTrainerDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $trainer_id = this.getTrainer_id();
        result = result * 59 + ($trainer_id == null ? 43 : $trainer_id.hashCode());
        String $full_name = this.getFull_name();
        result = result * 59 + ($full_name == null ? 43 : $full_name.hashCode());
        String $specialization = this.getSpecialization();
        result = result * 59 + ($specialization == null ? 43 : $specialization.hashCode());
        String $image_url = this.getImage_url();
        result = result * 59 + ($image_url == null ? 43 : $image_url.hashCode());
        return result;
    }

    public String toString() {
        return "GymTrainerDto(trainer_id=" + this.getTrainer_id() + ", full_name=" + this.getFull_name() + ", specialization=" + this.getSpecialization() + ", image_url=" + this.getImage_url() + ")";
    }

    public GymTrainerDto() {
    }

    public GymTrainerDto(String trainer_id, String full_name, String specialization, String image_url) {
        this.trainer_id = trainer_id;
        this.full_name = full_name;
        this.specialization = specialization;
        this.image_url = image_url;
    }

    public static class GymTrainerDtoBuilder {
        private String trainer_id;
        private String full_name;
        private String specialization;
        private String image_url;

        GymTrainerDtoBuilder() {
        }

        public GymTrainerDtoBuilder trainer_id(String trainer_id) {
            this.trainer_id = trainer_id;
            return this;
        }

        public GymTrainerDtoBuilder full_name(String full_name) {
            this.full_name = full_name;
            return this;
        }

        public GymTrainerDtoBuilder specialization(String specialization) {
            this.specialization = specialization;
            return this;
        }

        public GymTrainerDtoBuilder image_url(String image_url) {
            this.image_url = image_url;
            return this;
        }

        public GymTrainerDto build() {
            return new GymTrainerDto(this.trainer_id, this.full_name, this.specialization, this.image_url);
        }

        public String toString() {
            return "GymTrainerDto.GymTrainerDtoBuilder(trainer_id=" + this.trainer_id + ", full_name=" + this.full_name + ", specialization=" + this.specialization + ", image_url=" + this.image_url + ")";
        }
    }
}


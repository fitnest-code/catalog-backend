/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.GymPlanItemDto;
import az.fitnest.catalog.dto.GymReviewDto;
import az.fitnest.catalog.dto.GymRoomDto;
import az.fitnest.catalog.dto.GymTrainerDto;
import az.fitnest.catalog.dto.GymWorkHourDto;
import java.util.List;

public class GymDetailResponse {
    private String gym_id;
    private String name;
    private String description;
    private String status;
    private String address;
    private String phone;
    private String email;
    private List<GymWorkHourDto> work_hours;
    private List<GymRoomDto> rooms;
    private List<GymPlanItemDto> membership_plans;
    private List<GymTrainerDto> trainers;
    private List<GymReviewDto> recent_reviews;
    private String qr_code_url;

    public static GymDetailResponseBuilder builder() {
        return new GymDetailResponseBuilder();
    }

    public String getGym_id() {
        return this.gym_id;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getStatus() {
        return this.status;
    }

    public String getAddress() {
        return this.address;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getEmail() {
        return this.email;
    }

    public List<GymWorkHourDto> getWork_hours() {
        return this.work_hours;
    }

    public List<GymRoomDto> getRooms() {
        return this.rooms;
    }

    public List<GymPlanItemDto> getMembership_plans() {
        return this.membership_plans;
    }

    public List<GymTrainerDto> getTrainers() {
        return this.trainers;
    }

    public List<GymReviewDto> getRecent_reviews() {
        return this.recent_reviews;
    }

    public String getQr_code_url() {
        return this.qr_code_url;
    }

    public void setGym_id(String gym_id) {
        this.gym_id = gym_id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setWork_hours(List<GymWorkHourDto> work_hours) {
        this.work_hours = work_hours;
    }

    public void setRooms(List<GymRoomDto> rooms) {
        this.rooms = rooms;
    }

    public void setMembership_plans(List<GymPlanItemDto> membership_plans) {
        this.membership_plans = membership_plans;
    }

    public void setTrainers(List<GymTrainerDto> trainers) {
        this.trainers = trainers;
    }

    public void setRecent_reviews(List<GymReviewDto> recent_reviews) {
        this.recent_reviews = recent_reviews;
    }

    public void setQr_code_url(String qr_code_url) {
        this.qr_code_url = qr_code_url;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GymDetailResponse)) {
            return false;
        }
        GymDetailResponse other = (GymDetailResponse)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$gym_id = this.getGym_id();
        String other$gym_id = other.getGym_id();
        if (this$gym_id == null ? other$gym_id != null : !this$gym_id.equals(other$gym_id)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$address = this.getAddress();
        String other$address = other.getAddress();
        if (this$address == null ? other$address != null : !this$address.equals(other$address)) {
            return false;
        }
        String this$phone = this.getPhone();
        String other$phone = other.getPhone();
        if (this$phone == null ? other$phone != null : !this$phone.equals(other$phone)) {
            return false;
        }
        String this$email = this.getEmail();
        String other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) {
            return false;
        }
        List<GymWorkHourDto> this$work_hours = this.getWork_hours();
        List<GymWorkHourDto> other$work_hours = other.getWork_hours();
        if (this$work_hours == null ? other$work_hours != null : !((Object)this$work_hours).equals(other$work_hours)) {
            return false;
        }
        List<GymRoomDto> this$rooms = this.getRooms();
        List<GymRoomDto> other$rooms = other.getRooms();
        if (this$rooms == null ? other$rooms != null : !((Object)this$rooms).equals(other$rooms)) {
            return false;
        }
        List<GymPlanItemDto> this$membership_plans = this.getMembership_plans();
        List<GymPlanItemDto> other$membership_plans = other.getMembership_plans();
        if (this$membership_plans == null ? other$membership_plans != null : !((Object)this$membership_plans).equals(other$membership_plans)) {
            return false;
        }
        List<GymTrainerDto> this$trainers = this.getTrainers();
        List<GymTrainerDto> other$trainers = other.getTrainers();
        if (this$trainers == null ? other$trainers != null : !((Object)this$trainers).equals(other$trainers)) {
            return false;
        }
        List<GymReviewDto> this$recent_reviews = this.getRecent_reviews();
        List<GymReviewDto> other$recent_reviews = other.getRecent_reviews();
        return !(this$recent_reviews == null ? other$recent_reviews != null : !((Object)this$recent_reviews).equals(other$recent_reviews));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GymDetailResponse;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $gym_id = this.getGym_id();
        result = result * 59 + ($gym_id == null ? 43 : $gym_id.hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $address = this.getAddress();
        result = result * 59 + ($address == null ? 43 : $address.hashCode());
        String $phone = this.getPhone();
        result = result * 59 + ($phone == null ? 43 : $phone.hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        List<GymWorkHourDto> $work_hours = this.getWork_hours();
        result = result * 59 + ($work_hours == null ? 43 : ((Object)$work_hours).hashCode());
        List<GymRoomDto> $rooms = this.getRooms();
        result = result * 59 + ($rooms == null ? 43 : ((Object)$rooms).hashCode());
        List<GymPlanItemDto> $membership_plans = this.getMembership_plans();
        result = result * 59 + ($membership_plans == null ? 43 : ((Object)$membership_plans).hashCode());
        List<GymTrainerDto> $trainers = this.getTrainers();
        result = result * 59 + ($trainers == null ? 43 : ((Object)$trainers).hashCode());
        List<GymReviewDto> $recent_reviews = this.getRecent_reviews();
        result = result * 59 + ($recent_reviews == null ? 43 : ((Object)$recent_reviews).hashCode());
        return result;
    }

    public String toString() {
        return "GymDetailResponse(gym_id=" + this.getGym_id() + ", name=" + this.getName() + ", description=" + this.getDescription() + ", status=" + this.getStatus() + ", address=" + this.getAddress() + ", phone=" + this.getPhone() + ", email=" + this.getEmail() + ", work_hours=" + this.getWork_hours() + ", rooms=" + this.getRooms() + ", membership_plans=" + this.getMembership_plans() + ", trainers=" + this.getTrainers() + ", recent_reviews=" + this.getRecent_reviews() + ")";
    }

    public GymDetailResponse() {
    }

    public GymDetailResponse(String gym_id, String name, String description, String status, String address, String phone, String email, List<GymWorkHourDto> work_hours, List<GymRoomDto> rooms, List<GymPlanItemDto> membership_plans, List<GymTrainerDto> trainers, List<GymReviewDto> recent_reviews, String qr_code_url) {
        this.gym_id = gym_id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.work_hours = work_hours;
        this.rooms = rooms;
        this.membership_plans = membership_plans;
        this.trainers = trainers;
        this.recent_reviews = recent_reviews;
        this.qr_code_url = qr_code_url;
    }

    public static class GymDetailResponseBuilder {
        private String gym_id;
        private String name;
        private String description;
        private String status;
        private String address;
        private String phone;
        private String email;
        private List<GymWorkHourDto> work_hours;
        private List<GymRoomDto> rooms;
        private List<GymPlanItemDto> membership_plans;
        private List<GymTrainerDto> trainers;
        private List<GymReviewDto> recent_reviews;
        private String qr_code_url;

        GymDetailResponseBuilder() {
        }

        public GymDetailResponseBuilder gym_id(String gym_id) {
            this.gym_id = gym_id;
            return this;
        }

        public GymDetailResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public GymDetailResponseBuilder description(String description) {
            this.description = description;
            return this;
        }

        public GymDetailResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public GymDetailResponseBuilder address(String address) {
            this.address = address;
            return this;
        }

        public GymDetailResponseBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public GymDetailResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public GymDetailResponseBuilder work_hours(List<GymWorkHourDto> work_hours) {
            this.work_hours = work_hours;
            return this;
        }

        public GymDetailResponseBuilder rooms(List<GymRoomDto> rooms) {
            this.rooms = rooms;
            return this;
        }

        public GymDetailResponseBuilder membership_plans(List<GymPlanItemDto> membership_plans) {
            this.membership_plans = membership_plans;
            return this;
        }

        public GymDetailResponseBuilder trainers(List<GymTrainerDto> trainers) {
            this.trainers = trainers;
            return this;
        }

        public GymDetailResponseBuilder recent_reviews(List<GymReviewDto> recent_reviews) {
            this.recent_reviews = recent_reviews;
            return this;
        }

        public GymDetailResponseBuilder qr_code_url(String qr_code_url) {
            this.qr_code_url = qr_code_url;
            return this;
        }

        public GymDetailResponse build() {
            return new GymDetailResponse(this.gym_id, this.name, this.description, this.status, this.address, this.phone, this.email, this.work_hours, this.rooms, this.membership_plans, this.trainers, this.recent_reviews, this.qr_code_url);
        }

        public String toString() {
            return "GymDetailResponse.GymDetailResponseBuilder(gym_id=" + this.gym_id + ", name=" + this.name + ", description=" + this.description + ", status=" + this.status + ", address=" + this.address + ", phone=" + this.phone + ", email=" + this.email + ", work_hours=" + this.work_hours + ", rooms=" + this.rooms + ", membership_plans=" + this.membership_plans + ", trainers=" + this.trainers + ", recent_reviews=" + this.recent_reviews + ")";
        }
    }
}


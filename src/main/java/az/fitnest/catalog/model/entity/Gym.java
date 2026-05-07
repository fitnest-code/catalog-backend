package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.BaseAuditableEntity;
import az.fitnest.catalog.model.entity.Category;
import az.fitnest.catalog.model.entity.GymImage;
import az.fitnest.catalog.model.entity.GymSocialLink;
import az.fitnest.catalog.model.entity.GymWorkHour;
import az.fitnest.catalog.model.entity.Review;
import az.fitnest.catalog.model.entity.Trainer;
import az.fitnest.catalog.model.enums.GymStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "gyms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Gym
        extends BaseAuditableEntity {
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    @Column(name = "daily_price")
    private Double dailyPrice;

    @Column(name = "cover_image_url")
    private String coverImageUrl;
    @Column(name = "qr_code_url")
    private String qrCodeUrl;
    @Column(name = "qr_code_value")
    private String qrCodeValue;
    @Column(name = "qr_code_token")
    private String qrCodeToken;
    @Embedded
    private Address address;
    @Column(name = "phone")
    private String phone;
    @Column(name = "email")
    private String email;
    @ElementCollection
    @CollectionTable(name = "gym_social_links", joinColumns = {@JoinColumn(name = "gym_id")})
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<GymSocialLink> socialLinks = new ArrayList<GymSocialLink>();
    @ElementCollection
    @CollectionTable(name = "gym_general_work_hours", joinColumns = {@JoinColumn(name = "gym_id")})
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<GymWorkHour> generalWorkHours = new java.util.HashSet<>();

    @ElementCollection
    @CollectionTable(name = "gym_work_hours_woman", joinColumns = {@JoinColumn(name = "gym_id")})
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<GymWorkHour> workHoursWoman = new java.util.HashSet<>();

    @ElementCollection
    @CollectionTable(name = "gym_work_hours_man", joinColumns = {@JoinColumn(name = "gym_id")})
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<GymWorkHour> workHoursMan = new java.util.HashSet<>();

    @ElementCollection
    @CollectionTable(name = "gym_rest_days", joinColumns = {@JoinColumn(name = "gym_id")})
    @Column(name = "period")
    @Enumerated(EnumType.ORDINAL)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<az.fitnest.catalog.model.enums.GymWorkHourPeriod> restDays = new java.util.HashSet<>();
    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "gym_id")
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<GymImage> images = new ArrayList<GymImage>();
    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "gym_id")
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<Trainer> trainers = new ArrayList<Trainer>();
    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "gym_id")
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<Review> reviews = new ArrayList<Review>();
    @OneToMany(mappedBy = "gym", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<GymSubscription> subscriptions = new HashSet<>();

    @OneToMany(mappedBy = "gym", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<az.fitnest.catalog.model.entity.Room> rooms = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "gym_categories", joinColumns = {@JoinColumn(name = "gym_id")}, inverseJoinColumns = {@JoinColumn(name = "category_id")})
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Category> categories = new HashSet<Category>();
    @Column(name = "rating")
    @Builder.Default
    private Double rating = 0.0;
    @Column(name = "reviews_count")
    @Builder.Default
    private Integer reviewsCount = 0;
    @Column(name = "is_new")
    @Builder.Default
    private Boolean isNew = false;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private GymStatus status = GymStatus.ACTIVE;

    @Column(name = "is_reservation_enabled")
    @Builder.Default
    private Boolean isReservationEnabled = false;

    @Column(name = "creation_step")
    @Builder.Default
    private Integer creationStep = 1;

    public Long getGymId() {
        return getId();
    }

    public String getQrCodeValue() {
        return qrCodeValue;
    }

    public void setQrCodeValue(String qrCodeValue) {
        this.qrCodeValue = qrCodeValue;
    }

    public String getQrCodeToken() {
        return qrCodeToken;
    }

    public void setQrCodeToken(String qrCodeToken) {
        this.qrCodeToken = qrCodeToken;
    }
}

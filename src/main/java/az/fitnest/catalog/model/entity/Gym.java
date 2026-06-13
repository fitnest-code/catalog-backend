package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.enums.GymStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
public class Gym extends BaseAuditableEntity {
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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
    @org.hibernate.annotations.BatchSize(size = 20)
    private Set<GymWorkHour> generalWorkHours = new java.util.HashSet<>();

    @ElementCollection
    @CollectionTable(name = "gym_work_hours_woman", joinColumns = {@JoinColumn(name = "gym_id")})
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @org.hibernate.annotations.BatchSize(size = 20)
    private Set<GymWorkHour> workHoursWoman = new java.util.HashSet<>();

    @ElementCollection
    @CollectionTable(name = "gym_work_hours_man", joinColumns = {@JoinColumn(name = "gym_id")})
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @org.hibernate.annotations.BatchSize(size = 20)
    private Set<GymWorkHour> workHoursMan = new java.util.HashSet<>();

    @ElementCollection
    @CollectionTable(name = "gym_rest_days", joinColumns = {@JoinColumn(name = "gym_id")})
    @Column(name = "period")
    @Enumerated(EnumType.ORDINAL)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @org.hibernate.annotations.BatchSize(size = 20)
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
    @org.hibernate.annotations.BatchSize(size = 20)
    private Set<GymSubscription> subscriptions = new HashSet<>();

    @OneToMany(mappedBy = "gym", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<az.fitnest.catalog.model.entity.Room> rooms = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "gym", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<GymCategory> gymCategories = new HashSet<>();

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

    public Set<Category> getCategories() {
        if (gymCategories == null) return new HashSet<>();
        return gymCategories.stream()
                .map(GymCategory::getCategory)
                .collect(java.util.stream.Collectors.toSet());
    }

    public Set<Category> getMainCategories() {
        if (gymCategories == null) return new HashSet<>();
        return gymCategories.stream()
                .filter(GymCategory::isMain)
                .map(GymCategory::getCategory)
                .collect(java.util.stream.Collectors.toSet());
    }
}

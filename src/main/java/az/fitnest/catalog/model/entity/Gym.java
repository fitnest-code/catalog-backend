/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  jakarta.persistence.CascadeType
 *  jakarta.persistence.CollectionTable
 *  jakarta.persistence.Column
 *  jakarta.persistence.ElementCollection
 *  jakarta.persistence.Embedded
 *  jakarta.persistence.Entity
 *  jakarta.persistence.EnumType
 *  jakarta.persistence.Enumerated
 *  jakarta.persistence.JoinColumn
 *  jakarta.persistence.JoinTable
 *  jakarta.persistence.ManyToMany
 *  jakarta.persistence.OneToMany
 *  jakarta.persistence.Table
 */
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

    @Column(name = "cover_image_url")
    private String coverImageUrl;
    @Column(name = "qr_code_url")
    private String qrCodeUrl;
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
    @CollectionTable(name = "gym_work_hours", joinColumns = {@JoinColumn(name = "gym_id")})
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<GymWorkHour> workHours = new ArrayList<GymWorkHour>();
    
    @ElementCollection
    @CollectionTable(name = "gym_work_hours_woman", joinColumns = {@JoinColumn(name = "gym_id")})
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<GymWorkHour> workHoursWoman = new ArrayList<GymWorkHour>();
    
    @ElementCollection
    @CollectionTable(name = "gym_work_hours_man", joinColumns = {@JoinColumn(name = "gym_id")})
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<GymWorkHour> workHoursMan = new ArrayList<GymWorkHour>();
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
    private List<GymSubscription> subscriptions = new ArrayList<GymSubscription>();
    
    @OneToMany(mappedBy = "gym", cascade = {CascadeType.ALL}, orphanRemoval = true)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<GymAdmin> admins = new ArrayList<GymAdmin>();

    @OneToMany(mappedBy = "gym", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<az.fitnest.catalog.model.entity.Room> rooms = new ArrayList<>();
    
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

    public Long getGymId() {
        return getId();
    }
}

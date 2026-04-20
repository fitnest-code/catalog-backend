package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.enums.AdminPanelGymStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "gyms_admin_panel")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class GymAdminPanel extends BaseAuditableEntity {

    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Embedded
    private AddressAdminPanel address;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "gym", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<AdminPanelWorkingHour> workingHours = new ArrayList<>();

    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "gym_id")
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<AdminPanelGymImage> images = new ArrayList<AdminPanelGymImage>();

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

    @Column(name = "rating")
    @Builder.Default
    private Double rating = 0.0;

    @Column(name = "reviews_count")
    @Builder.Default
    private Integer reviewsCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AdminPanelGymStatus status = AdminPanelGymStatus.ACTIVE;

    public Long getGymId() {
        return getId();
    }
}

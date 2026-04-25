package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.enums.RatingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@Table(name = "gym_ratings")
public class GymRating extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private GymAdminPanel gym;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_full_name")
    private String customerFullName;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RatingStatus status = RatingStatus.PENDING;

    @Column(name = "moderation_note")
    private String moderationNote;

    @Column(name = "moderated_by")
    private Long moderatedBy;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;
}
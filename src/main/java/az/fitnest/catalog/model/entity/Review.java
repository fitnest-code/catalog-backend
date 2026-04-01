package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "reviews", indexes = {@Index(name = "idx_reviews_user_id", columnList = "user_id")})
public class Review
        extends BaseAuditableEntity {
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "gym_id", insertable = false, updatable = false)
    private Long gymId;
    @ManyToOne
    @JoinColumn(name = "gym_id", referencedColumnName = "id")
    private Gym gym;
    @Column(name = "rating")
    private Integer rating;
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "status", nullable = false)
    private az.fitnest.catalog.model.enums.ReviewStatus status = az.fitnest.catalog.model.enums.ReviewStatus.PENDING;

    public Review() {
    }

    public Review(Long userId, Integer rating, String comment) {
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGymId() {
        return this.gymId;
    }

    public void setGymId(Long gymId) {
        this.gymId = gymId;
    }

    public Integer getRating() {
        return this.rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return this.comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Gym getGym() {
        return gym;
    }

    public void setGym(Gym gym) {
        this.gym = gym;
    }

    public az.fitnest.catalog.model.enums.ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(az.fitnest.catalog.model.enums.ReviewStatus status) {
        this.status = status;
    }
}

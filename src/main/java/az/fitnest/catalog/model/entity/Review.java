/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Index
 *  jakarta.persistence.Table
 */
package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name="reviews", indexes={@Index(name="idx_reviews_user_id", columnList="user_id")})
public class Review
extends BaseAuditableEntity {
    @Column(name="user_id")
    private Long userId;
    @Column(name="gym_id", updatable=false, insertable=false)
    private Long gymId;
    @Column(name="rating")
    private Integer rating;
    @Column(name="comment", columnDefinition="TEXT")
    private String comment;

    public Long getUserId() {
        return this.userId;
    }

    public Long getGymId() {
        return this.gymId;
    }

    public Integer getRating() {
        return this.rating;
    }

    public String getComment() {
        return this.comment;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setGymId(Long gymId) {
        this.gymId = gymId;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Review() {
    }

    public Review(Long userId, Integer rating, String comment) {
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
    }
}


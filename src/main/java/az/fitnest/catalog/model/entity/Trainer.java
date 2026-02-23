/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 */
package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name="trainers")
public class Trainer
extends BaseAuditableEntity {
    @Column(name="full_name")
    private String fullName;
    @Column(name="specialization")
    private String specialization;
    @Column(name="image_url")
    private String imageUrl;

    public String getFullName() {
        return this.fullName;
    }

    public String getSpecialization() {
        return this.specialization;
    }

    public String getImageUrl() {
        return this.imageUrl;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Trainer() {
    }

    public Trainer(String fullName, String specialization, String imageUrl) {
        this.fullName = fullName;
        this.specialization = specialization;
        this.imageUrl = imageUrl;
    }
}


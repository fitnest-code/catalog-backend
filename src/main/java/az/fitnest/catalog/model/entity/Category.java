/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.ManyToMany
 *  jakarta.persistence.Table
 */
package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.BaseAuditableEntity;
import az.fitnest.catalog.model.entity.Gym;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name="categories")
public class Category
extends BaseAuditableEntity {
    @Column(name="name", nullable=false, unique=true)
    private String name;
    @Column(name="photo_url")
    private String photoUrl;
    @ManyToMany(mappedBy="categories")
    private Set<Gym> gyms = new HashSet<Gym>();

    public String getName() {
        return this.name;
    }

    public String getPhotoUrl() {
        return this.photoUrl;
    }

    public Set<Gym> getGyms() {
        return this.gyms;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public void setGyms(Set<Gym> gyms) {
        this.gyms = gyms;
    }

    public Category() {
    }

    public Category(String name, String photoUrl, Set<Gym> gyms) {
        this.name = name;
        this.photoUrl = photoUrl;
        this.gyms = gyms;
    }
}


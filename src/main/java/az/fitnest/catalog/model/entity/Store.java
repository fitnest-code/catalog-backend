/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.persistence.AttributeOverride
 *  jakarta.persistence.AttributeOverrides
 *  jakarta.persistence.CascadeType
 *  jakarta.persistence.CollectionTable
 *  jakarta.persistence.Column
 *  jakarta.persistence.ElementCollection
 *  jakarta.persistence.Embedded
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Index
 *  jakarta.persistence.JoinColumn
 *  jakarta.persistence.OneToMany
 *  jakarta.persistence.OneToOne
 *  jakarta.persistence.PrimaryKeyJoinColumn
 *  jakarta.persistence.Table
 */
package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.BaseAuditableEntity;
import az.fitnest.catalog.model.entity.StoreAddress;
import az.fitnest.catalog.model.entity.StoreDiscount;
import az.fitnest.catalog.model.entity.StoreImage;
import az.fitnest.catalog.model.entity.StoreSocialLink;
import az.fitnest.catalog.model.entity.StoreWorkHours;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name="stores", indexes={@Index(name="idx_stores_status", columnList="status"), @Index(name="idx_stores_category", columnList="category")})
public class Store
extends BaseAuditableEntity {
    @Column(name="name", nullable=false)
    private String name;
    @Column(name="description", columnDefinition="TEXT")
    private String description;
    @Column(name="category")
    private String category;
    @Column(name="status")
    private String status;
    @Embedded
    @AttributeOverrides(value={@AttributeOverride(name="addressText", column=@Column(name="address_text")), @AttributeOverride(name="latitude", column=@Column(name="address_lat")), @AttributeOverride(name="longitude", column=@Column(name="address_lng"))})
    private StoreAddress address;
    @Column(name="phone")
    private String phone;
    @ElementCollection
    @CollectionTable(name="store_work_hours", joinColumns={@JoinColumn(name="store_id")})
    private List<StoreWorkHours> workHours = new ArrayList<StoreWorkHours>();
    @Column(name="logo_url")
    private String logoUrl;
    @Column(name="cover_image_url")
    private String coverImageUrl;
    @Column(name="popular_score")
    private Double popularScore = 0.0;
    @ElementCollection
    @CollectionTable(name="store_badges", joinColumns={@JoinColumn(name="store_id")})
    @Column(name="badge")
    private List<String> badges = new ArrayList<String>();
    @OneToMany(cascade={CascadeType.ALL}, orphanRemoval=true)
    @JoinColumn(name="store_id")
    private Set<StoreDiscount> discounts = new LinkedHashSet<StoreDiscount>();
    @OneToMany(cascade={CascadeType.ALL}, orphanRemoval=true)
    @JoinColumn(name="store_id")
    private Set<StoreImage> images = new LinkedHashSet<StoreImage>();
    @ElementCollection
    @CollectionTable(name = "store_social_links", joinColumns = @JoinColumn(name = "store_id"))
    private List<StoreSocialLink> socialLinks = new ArrayList<>();

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getCategory() {
        return this.category;
    }

    public String getStatus() {
        return this.status;
    }

    public StoreAddress getAddress() {
        return this.address;
    }

    public String getPhone() {
        return this.phone;
    }

    public List<StoreWorkHours> getWorkHours() {
        return this.workHours;
    }

    public String getLogoUrl() {
        return this.logoUrl;
    }

    public String getCoverImageUrl() {
        return this.coverImageUrl;
    }

    public Double getPopularScore() {
        return this.popularScore;
    }

    public List<String> getBadges() {
        return this.badges;
    }

    public Set<StoreDiscount> getDiscounts() {
        return this.discounts;
    }

    public Set<StoreImage> getImages() {
        return this.images;
    }

    public List<StoreSocialLink> getSocialLinks() {
        return this.socialLinks;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAddress(StoreAddress address) {
        this.address = address;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setWorkHours(List<StoreWorkHours> workHours) {
        this.workHours = workHours;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public void setPopularScore(Double popularScore) {
        this.popularScore = popularScore;
    }

    public void setBadges(List<String> badges) {
        this.badges = badges;
    }

    public void setDiscounts(Set<StoreDiscount> discounts) {
        this.discounts = discounts;
    }

    public void setImages(Set<StoreImage> images) {
        this.images = images;
    }

    public void setSocialLinks(List<StoreSocialLink> socialLinks) {
        this.socialLinks = socialLinks;
    }

    public Store() {
    }

    public Store(String name, String description, String category, String status, StoreAddress address, String phone, List<StoreWorkHours> workHours, String logoUrl, String coverImageUrl, Double popularScore, List<String> badges, Set<StoreDiscount> discounts, Set<StoreImage> images, List<StoreSocialLink> socialLinks) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.status = status;
        this.address = address;
        this.phone = phone;
        this.workHours = workHours;
        this.logoUrl = logoUrl;
        this.coverImageUrl = coverImageUrl;
        this.popularScore = popularScore;
        this.badges = badges;
        this.discounts = discounts;
        this.images = images;
        this.socialLinks = socialLinks;
    }
}


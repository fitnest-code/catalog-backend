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
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name="gyms")
public class Gym
extends BaseAuditableEntity {
    @Column(name="name", nullable=false)
    private String name;
    @Column(name="description", columnDefinition="TEXT")
    private String description;
    @Enumerated(value=EnumType.STRING)
    @Column(name="status")
    private GymStatus status = GymStatus.ACTIVE;
    @Column(name="cover_image_url")
    private String coverImageUrl;
    @Column(name="logo_url")
    private String logoUrl;
    @Embedded
    private Address address;
    @Column(name="phone")
    private String phone;
    @Column(name="email")
    private String email;
    @ElementCollection
    @CollectionTable(name="gym_social_links", joinColumns={@JoinColumn(name="gym_id")})
    private List<GymSocialLink> socialLinks = new ArrayList<GymSocialLink>();
    @ElementCollection
    @CollectionTable(name="gym_work_hours", joinColumns={@JoinColumn(name="gym_id")})
    private List<GymWorkHour> workHours = new ArrayList<GymWorkHour>();
    @OneToMany(cascade={CascadeType.ALL}, orphanRemoval=true)
    @JoinColumn(name="gym_id")
    private List<GymImage> images = new ArrayList<GymImage>();
    @OneToMany(cascade={CascadeType.ALL}, orphanRemoval=true)
    @JoinColumn(name="gym_id")
    private List<Trainer> trainers = new ArrayList<Trainer>();
    @OneToMany(cascade={CascadeType.ALL}, orphanRemoval=true)
    @JoinColumn(name="gym_id")
    private List<Review> reviews = new ArrayList<Review>();
    @ManyToMany
    @JoinTable(name="gym_categories", joinColumns={@JoinColumn(name="gym_id")}, inverseJoinColumns={@JoinColumn(name="category_id")})
    private Set<Category> categories = new HashSet<Category>();
    @Column(name="rating")
    private Double rating = 0.0;
    @Column(name="reviews_count")
    private Integer reviewsCount = 0;
    @Column(name="is_new")
    private Boolean isNew = false;

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public GymStatus getStatus() {
        return this.status;
    }

    public String getCoverImageUrl() {
        return this.coverImageUrl;
    }

    public String getLogoUrl() {
        return this.logoUrl;
    }

    public Address getAddress() {
        return this.address;
    }

    public String getPhone() {
        return this.phone;
    }

    public String getEmail() {
        return this.email;
    }

    public List<GymSocialLink> getSocialLinks() {
        return this.socialLinks;
    }

    public List<GymWorkHour> getWorkHours() {
        return this.workHours;
    }

    public List<GymImage> getImages() {
        return this.images;
    }

    public List<Trainer> getTrainers() {
        return this.trainers;
    }

    public List<Review> getReviews() {
        return this.reviews;
    }

    public Set<Category> getCategories() {
        return this.categories;
    }

    public Double getRating() {
        return this.rating;
    }

    public Integer getReviewsCount() {
        return this.reviewsCount;
    }

    public Boolean getIsNew() {
        return this.isNew;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(GymStatus status) {
        this.status = status;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setSocialLinks(List<GymSocialLink> socialLinks) {
        this.socialLinks = socialLinks;
    }

    public void setWorkHours(List<GymWorkHour> workHours) {
        this.workHours = workHours;
    }

    public void setImages(List<GymImage> images) {
        this.images = images;
    }

    public void setTrainers(List<Trainer> trainers) {
        this.trainers = trainers;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public void setCategories(Set<Category> categories) {
        this.categories = categories;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public void setReviewsCount(Integer reviewsCount) {
        this.reviewsCount = reviewsCount;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }

    public Gym() {
    }

    public Gym(String name, String description, GymStatus status, String coverImageUrl, String logoUrl, Address address, String phone, String email, List<GymSocialLink> socialLinks, List<GymWorkHour> workHours, List<GymImage> images, List<Trainer> trainers, List<Review> reviews, Set<Category> categories, Double rating, Integer reviewsCount, Boolean isNew) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.coverImageUrl = coverImageUrl;
        this.logoUrl = logoUrl;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.socialLinks = socialLinks;
        this.workHours = workHours;
        this.images = images;
        this.trainers = trainers;
        this.reviews = reviews;
        this.categories = categories;
        this.rating = rating;
        this.reviewsCount = reviewsCount;
        this.isNew = isNew;
    }
}


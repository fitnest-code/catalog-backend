package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.Address;
import az.fitnest.catalog.model.entity.BaseAuditableEntity;
import az.fitnest.catalog.model.entity.StoreDiscount;
import az.fitnest.catalog.model.entity.StoreImage;
import az.fitnest.catalog.model.entity.StoreSocialLink;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "stores", indexes = {@Index(name = "idx_stores_status", columnList = "status"), @Index(name = "idx_stores_category", columnList = "category")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Store
        extends BaseAuditableEntity {
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "category")
    private String category;
    @Column(name = "status")
    private String status;
    @Embedded
    @AttributeOverrides(value = {
        @AttributeOverride(name = "addressText", column = @Column(name = "address_text")),
        @AttributeOverride(name = "city", column = @Column(name = "city")),
        @AttributeOverride(name = "latitude", column = @Column(name = "address_lat")),
        @AttributeOverride(name = "longitude", column = @Column(name = "address_lng"))
    })
    private Address address;
    @Column(name = "phone")
    private String phone;

    @Column(name = "logo_url")
    private String logoUrl;
    @Column(name = "cover_image_url")
    private String coverImageUrl;
    @Column(name = "popular_score")
    @Builder.Default
    private Double popularScore = 0.0;
    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "store_id")
    @Builder.Default
    private Set<StoreDiscount> discounts = new LinkedHashSet<StoreDiscount>();
    @OneToMany(cascade = {CascadeType.ALL}, orphanRemoval = true)
    @JoinColumn(name = "store_id")
    @Builder.Default
    private Set<StoreImage> images = new LinkedHashSet<StoreImage>();
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "name", column = @Column(name = "social_name")),
        @AttributeOverride(name = "url", column = @Column(name = "social_url"))
    })
    private StoreSocialLink socialLink;

    public Long getStoreId() {
        return getId();
    }
}

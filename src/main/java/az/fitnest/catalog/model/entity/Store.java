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
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name="stores", indexes={@Index(name="idx_stores_status", columnList="status"), @Index(name="idx_stores_category", columnList="category")})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Store
extends BaseAuditableEntity {
    @Column(name="name", nullable=false)
    private String name;
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
    @Builder.Default
    private Set<StoreWorkHours> workHours = new LinkedHashSet<>();
    @Column(name="logo_url")
    private String logoUrl;
    @Column(name="cover_image_url")
    private String coverImageUrl;
    @Column(name="popular_score")
    @Builder.Default
    private Double popularScore = 0.0;
    @OneToMany(cascade={CascadeType.ALL}, orphanRemoval=true)
    @JoinColumn(name="store_id")
    @Builder.Default
    private Set<StoreDiscount> discounts = new LinkedHashSet<StoreDiscount>();
    @OneToMany(cascade={CascadeType.ALL}, orphanRemoval=true)
    @JoinColumn(name="store_id")
    @Builder.Default
    private Set<StoreImage> images = new LinkedHashSet<StoreImage>();
    @ElementCollection
    @CollectionTable(name = "store_social_links", joinColumns = @JoinColumn(name = "store_id"))
    @Builder.Default
    private Set<StoreSocialLink> socialLinks = new LinkedHashSet<>();

    public Long getStoreId() {
        return getId();
    }
}


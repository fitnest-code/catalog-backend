package az.fitnest.catalog.model.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
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
    @Column(name = "email")
    private String email;

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

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "day",      column = @Column(name = "work_hours_day")),
            @AttributeOverride(name = "fromTime", column = @Column(name = "work_hours_from")),
            @AttributeOverride(name = "toTime",   column = @Column(name = "work_hours_to"))
    })
    private StoreWorkHours workHours;

    public Long getStoreId() {
        return getId();
    }
}

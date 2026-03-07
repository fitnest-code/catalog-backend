package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.BaseAuditableEntity;
import az.fitnest.catalog.model.entity.Gym;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Category
        extends BaseAuditableEntity {
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    @Column(name = "photo_url")
    private String photoUrl;
    @ManyToMany(mappedBy = "categories")
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Gym> gyms = new HashSet<Gym>();

    public Long getCategoryId() {
        return getId();
    }
}

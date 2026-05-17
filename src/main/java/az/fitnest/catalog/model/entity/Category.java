package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.entity.BaseAuditableEntity;
import az.fitnest.catalog.model.entity.Gym;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.JoinTable;
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

    @Column(name = "icon_url")
    private String iconUrl;
    @jakarta.persistence.OneToMany(mappedBy = "category")
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Gym> gyms = new HashSet<Gym>();

    @ManyToMany
    @JoinTable(
        name = "category_lesson_types",
        joinColumns = @jakarta.persistence.JoinColumn(name = "category_id"),
        inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "lesson_type_id")
    )
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<LessonType> lessonTypes = new HashSet<>();

    public Long getCategoryId() {
        return getId();
    }
}

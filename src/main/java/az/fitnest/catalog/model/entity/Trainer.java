package az.fitnest.catalog.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trainers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Trainer extends BaseAuditableEntity {

    @Column(name = "gym_id", insertable = false, updatable = false)
    private Long gymId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "specialization")
    private String specialization;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "rating")
    @Builder.Default
    private Double rating = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profession_id")
    private Profession profession;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    public String getName() {
        return firstName;
    }

    public void setName(String name) {
        this.firstName = name;
    }

    public String getSurname() {
        return lastName;
    }

    public void setSurname(String surname) {
        this.lastName = surname;
    }

    public String getPicture() {
        return profileImageUrl;
    }

    public void setPicture(String picture) {
        this.profileImageUrl = picture;
    }

    public Long getTrainerId() {
        return getId();
    }



    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "trainer_enabled_lessons",
        joinColumns = @JoinColumn(name = "trainer_id"),
        inverseJoinColumns = @JoinColumn(name = "lesson_type_id")
    )
    @Builder.Default
    private java.util.Set<LessonType> enabledLessonTypes = new java.util.HashSet<>();
}

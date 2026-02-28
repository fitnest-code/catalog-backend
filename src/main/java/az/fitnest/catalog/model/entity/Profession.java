package az.fitnest.catalog.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "professions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Profession extends BaseAuditableEntity {
    
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    public Long getProfessionId() {
        return getId();
    }
}

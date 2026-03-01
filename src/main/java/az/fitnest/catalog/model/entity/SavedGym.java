package az.fitnest.catalog.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_gyms",
        indexes = {@Index(name = "idx_saved_gyms_user_id", columnList = "user_id")},
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "gym_id"})})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedGym {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;
}

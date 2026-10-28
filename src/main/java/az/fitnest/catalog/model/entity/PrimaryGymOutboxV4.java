package az.fitnest.catalog.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "primary_gym_outbox_v4")
@Getter
@Setter
@NoArgsConstructor
public class PrimaryGymOutboxV4 {
  @Id private String id;

  @Column(nullable = false, columnDefinition = "text")
  private String payload;

  private boolean published;
}

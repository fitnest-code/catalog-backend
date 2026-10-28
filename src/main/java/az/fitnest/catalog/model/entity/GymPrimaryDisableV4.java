package az.fitnest.catalog.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "gym_primary_disable_v4")
@Getter
@Setter
@NoArgsConstructor
public class GymPrimaryDisableV4 {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long gymId;
  private Long packageId;
  private LocalDateTime disabledAt;
}

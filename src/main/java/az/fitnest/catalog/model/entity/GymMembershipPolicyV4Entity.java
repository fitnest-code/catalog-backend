package az.fitnest.catalog.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gym_membership_policy_v4")
@Getter
@Setter
@NoArgsConstructor
public class GymMembershipPolicyV4Entity {
  @Id private Long gymId;
  @Version private long version;

  @Column(nullable = false, columnDefinition = "text")
  private String payload;
}

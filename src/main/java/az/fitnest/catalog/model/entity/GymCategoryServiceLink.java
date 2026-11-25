package az.fitnest.catalog.model.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import lombok.*;

@Entity
@Table(name = "gym_category_services")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(GymCategoryServiceLink.Pk.class)
public class GymCategoryServiceLink {

    @Id
    @Column(name = "gym_id", nullable = false)
    private Long gymId;

    @Id
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Id
    @Column(name = "supported_service_id", nullable = false)
    private Long supportedServiceId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pk implements Serializable {
        private Long gymId;
        private Long categoryId;
        private Long supportedServiceId;
    }
}

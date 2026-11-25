package az.fitnest.catalog.model.entity;

import az.fitnest.catalog.model.enums.GymWorkHourPeriod;
import jakarta.persistence.*;
import java.time.LocalTime;
import lombok.*;

@Entity
@Table(name = "gym_category_work_hours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GymCategoryWorkHour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gym_id", nullable = false)
    private Long gymId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "period", nullable = false)
    private GymWorkHourPeriod period;

    @Column(name = "from_time")
    private LocalTime fromTime;

    @Column(name = "to_time")
    private LocalTime toTime;
}

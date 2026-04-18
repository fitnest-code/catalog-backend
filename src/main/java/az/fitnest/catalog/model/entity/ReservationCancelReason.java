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
@Table(name = "reservation_cancel_reasons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReservationCancelReason extends BaseAuditableEntity {

    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "requires_comment")
    @Builder.Default
    private Boolean requiresComment = false;

    @Column(name = "status")
    @Builder.Default
    private String status = "ACTIVE";
}

package az.fitnest.catalog.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "app_qr_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AppQrCode extends BaseAuditableEntity {

    @Column(name = "mode", nullable = false, unique = true)
    private String mode; // "LIGHT" or "DARK"

    @Column(name = "scan_count", nullable = false)
    @Builder.Default
    private Long scanCount = 0L;

    @Column(name = "qr_code_url")
    private String qrCodeUrl;
}

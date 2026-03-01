/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  jakarta.persistence.Embeddable
 */
package az.fitnest.catalog.model.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreAddress {
    private String addressText;
    private String city;
    private Double latitude;
    private Double longitude;
}


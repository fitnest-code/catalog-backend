/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GymPlanBenefitDto {
    private String logo;
    private String description;
}

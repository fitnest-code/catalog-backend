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
public class GymMainPageDto {
    private String gymId;
    private String name;
    private String coverImageUrl;
    private double stars;
    private boolean isNew;
    private String location;
    private String city;
    private Double distanceKm;
}


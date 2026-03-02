/*
 * Decompiled with CFR 0.152.
 */
package az.fitnest.catalog.dto;


import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GymSocialLinkDto {
    private String name;
    private String url;

}

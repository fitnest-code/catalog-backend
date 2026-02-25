/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.AddressDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class GymRequest {
    private String name;
    private String description;
    private AddressDto address;
    private String phone;
    private String email;
    private Set<Long> categoryIds;


}

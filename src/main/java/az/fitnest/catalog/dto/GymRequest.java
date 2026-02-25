/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.swagger.v3.oas.annotations.media.Schema
 */
package az.fitnest.catalog.dto;

import az.fitnest.catalog.dto.AddressDto;
import java.util.Set;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class GymRequest {
    @NotBlank(message = "Name cannot be empty")
    private String name;
    
    private String description;
    
    @NotNull(message = "Address cannot be null")
    @Valid
    private AddressDto address;
    
    @NotBlank(message = "Phone cannot be empty")
    @Pattern(regexp = "^(050|051|010|055|099|070|077|060)\\d{7}$", message = "Invalid mobile number format. Must start with 050, 051, 010, 055, 099, 070, 077, or 060 and follow with 7 digits.")
    private String phone;
    
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotEmpty(message = "Categories cannot be empty")
    private Set<Long> categoryIds;


}

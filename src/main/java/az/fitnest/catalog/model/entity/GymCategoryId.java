package az.fitnest.catalog.model.entity;

/**
 * @author: nijataghayev
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GymCategoryId implements Serializable {
    private Long gym;
    private Long category;
}

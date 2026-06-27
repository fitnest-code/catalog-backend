package az.fitnest.catalog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import java.util.List;

@Builder
public record GymCreateStep1RequestV2(
    @NotEmpty(message = "Kateqoriya tələb olunur")
    List<CategoryDetail> mainCategoryDetails,

    List<CategoryDetail> subCategoryDetails,

    @NotBlank(message = "Ad boş ola bilməz")
    @Size(min = 2, max = 100)
    String name,

    @NotBlank(message = "Telefon boş ola bilməz")
    @Pattern(regexp = "^(\\+994|0)?\\s?(10|50|51|55|60|70|77|99)(\\s?\\d){7}$", message = "Yanlış mobil nömrə formatı")
    String phone,

    @Size(max = 2000)
    String description,

    @Email(message = "Yanlış email formatı")
    String email,

    Boolean hasSubcategories,

    List<Long> lessonTypeIds
) {}

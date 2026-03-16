package az.fitnest.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description = "Sıralama istiqaməti. ASC (Artan sıra, A-dan Z-yə, ən aşağıdan ən yuxarıya), DESC (Azalan sıra, Z-dən A-ya, ən yuxarıdan ən aşağıya)",
    example = "ASC",
    allowableValues = {"ASC", "DESC"}
)
public enum SortDirection {
    ASC,
    DESC;
}

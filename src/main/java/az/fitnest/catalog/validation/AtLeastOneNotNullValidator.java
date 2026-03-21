package az.fitnest.catalog.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.RecordComponent;

public class AtLeastOneNotNullValidator implements ConstraintValidator<AtLeastOneNotNull, Object> {
    private String[] fields;

    @Override
    public void initialize(AtLeastOneNotNull constraintAnnotation) {
        this.fields = constraintAnnotation.fields();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return false;
        try {
            for (String field : fields) {
                Object fieldValue = null;
                if (value instanceof Record) {
                    RecordComponent[] components = value.getClass().getRecordComponents();
                    for (RecordComponent rc : components) {
                        if (rc.getName().equals(field)) {
                            fieldValue = rc.getAccessor().invoke(value);
                            break;
                        }
                    }
                } else {
                    fieldValue = value.getClass().getDeclaredField(field).get(value);
                }
                if (fieldValue != null) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}

package az.fitnest.catalog.exception;

import org.springframework.http.HttpStatus;
import java.util.List;

public class GymDependencyException extends BaseException {
    private final List<String> dependencyKeys;

    public GymDependencyException(List<String> dependencyKeys) {
        super("error.gym_has_dependencies", "GYM_HAS_DEPENDENCIES", HttpStatus.BAD_REQUEST);
        this.dependencyKeys = dependencyKeys;
    }

    public List<String> getDependencyKeys() {
        return dependencyKeys;
    }
}

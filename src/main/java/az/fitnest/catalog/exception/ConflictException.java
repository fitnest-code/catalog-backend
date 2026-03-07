package az.fitnest.catalog.exception;

import az.fitnest.catalog.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ConflictException
        extends BaseException {
    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message, "CONFLICT", HttpStatus.CONFLICT);
    }

    public ConflictException(String errorCode, String message) {
        super(message, errorCode, HttpStatus.CONFLICT);
    }
}

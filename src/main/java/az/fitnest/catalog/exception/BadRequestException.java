package az.fitnest.catalog.exception;

import az.fitnest.catalog.exception.BaseException;
import org.springframework.http.HttpStatus;

public class BadRequestException
        extends BaseException {
    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        super(message, "BAD_REQUEST", HttpStatus.BAD_REQUEST);
    }

    public BadRequestException(String errorCode, String message) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
    }
}

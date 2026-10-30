package az.fitnest.catalog.exception;

import org.springframework.http.HttpStatus;

public class UnprocessableEntityException extends BaseException {

    private static final long serialVersionUID = 1L;

    public UnprocessableEntityException(String errorCode, String message) {
        super(message, errorCode, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}

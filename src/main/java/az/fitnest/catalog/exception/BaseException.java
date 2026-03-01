/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.http.HttpStatus
 */
package az.fitnest.catalog.exception;

import org.springframework.http.HttpStatus;

public abstract class BaseException
        extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final HttpStatus httpStatus;
    private final String errorCode;

    protected BaseException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return this.httpStatus;
    }

    public String getErrorCode() {
        return this.errorCode;
    }
}


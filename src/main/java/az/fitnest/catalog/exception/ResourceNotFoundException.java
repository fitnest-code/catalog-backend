/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.http.HttpStatus
 */
package az.fitnest.catalog.exception;

import az.fitnest.catalog.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException
        extends BaseException {
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String errorCode, String message) {
        super(message, HttpStatus.NOT_FOUND, errorCode);
    }
}


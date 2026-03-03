/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.springframework.http.HttpStatus
 */
package az.fitnest.catalog.exception;

import az.fitnest.catalog.exception.BaseException;
import org.springframework.http.HttpStatus;

public class InternalServerException
        extends BaseException {
    private static final long serialVersionUID = 1L;

    public InternalServerException(String message) {
        super(message, "INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}


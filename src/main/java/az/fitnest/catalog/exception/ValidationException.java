/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.http.HttpStatus
 *  org.springframework.validation.BindingResult
 */
package az.fitnest.catalog.exception;

import az.fitnest.catalog.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;

public class ValidationException
extends BaseException {
    private static final long serialVersionUID = 1L;
    private final BindingResult bindingResult;

    public ValidationException(String message, BindingResult bindingResult) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        this.bindingResult = bindingResult;
    }

    public BindingResult getBindingResult() {
        return this.bindingResult;
    }
}


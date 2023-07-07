package com.redhat.agogos.core.errors;

public class ValidationException extends ApplicationException {

    private static final long serialVersionUID = -1031796522718839075L;

    public ValidationException(String msg, final Object... params) {
        super(msg, params);
    }
}

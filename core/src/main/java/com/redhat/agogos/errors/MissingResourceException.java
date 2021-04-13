package com.redhat.agogos.errors;

public class MissingResourceException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public MissingResourceException(String msg, final Object... params) {
        super(msg, params);
    }
}

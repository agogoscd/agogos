package com.redhat.agogos.errors;

import javax.ws.rs.core.Response.Status;

public class MissingResourceException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public MissingResourceException(String msg, final Object... params) {
        super(Status.NOT_FOUND, msg, params);
    }
}

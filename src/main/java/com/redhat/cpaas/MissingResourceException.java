package com.redhat.cpaas;

public class MissingResourceException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    public MissingResourceException() {
        super();
    }

    public MissingResourceException(String msg) {
        super(msg);
    }

    public MissingResourceException(String msg, Exception e) {
        super(msg, e);
    }
}

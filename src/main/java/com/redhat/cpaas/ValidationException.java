package com.redhat.cpaas;

import java.util.ArrayList;
import java.util.List;

import com.redhat.cpaas.model.Error;
import com.redhat.cpaas.model.ValidationError;

import lombok.Getter;
import lombok.Setter;

public class ValidationException extends ApplicationException {

    private static final long serialVersionUID = -1031796522718839075L;

    @Getter
    @Setter
    private List<String> errors;

    public ValidationException() {
        super();
    }

    public ValidationException(String msg) {
        super(msg);
    }

    public ValidationException(String msg, Exception e) {
        super(msg, e);
    }

    public ValidationException(String msg, List<String> messages) {
        super(msg);
        this.errors = new ArrayList<>(messages);
    }

    public ValidationException(String msg, List<String> messages, Exception e) {
        super(msg, e);
        this.errors = new ArrayList<>(messages);
    }

    @Override
    public Error toError() {
        return new ValidationError(this.getMessage(), this.getErrors());
    }

}

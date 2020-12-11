package com.redhat.cpaas.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class ValidationError extends Error {

    @Getter
    @Setter
    List<String> errors;

    public ValidationError() {
    }

    public ValidationError(String message) {
        super(message, "https://gitlab.cee.redhat.com/cpaas/documentation/-/tree/master/schema");
    }

    public ValidationError(String message, List<String> messages) {
        super(message, "https://gitlab.cee.redhat.com/cpaas/documentation/-/tree/master/schema");

        this.errors = new ArrayList<>(messages);
    }
}

package com.redhat.cpaas.errors;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import lombok.Getter;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApplicationException extends Exception {

    private static final long serialVersionUID = 1L;

    @Getter
    private List<String> messages = new ArrayList<>();

    public ApplicationException() {
        super();
    }

    public ApplicationException(String msg) {
        super(msg);
    }

    public ApplicationException(String msg, Exception e) {
        super(msg, e);
    }

    public Error toError() {
        return new Error(this.getMessage());
    }
}

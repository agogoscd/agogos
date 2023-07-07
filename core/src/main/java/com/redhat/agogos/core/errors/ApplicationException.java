package com.redhat.agogos.core.errors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import org.slf4j.helpers.MessageFormatter;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApplicationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Object[] params;

    private String formattedMessage;
    private final Integer statusCode;

    public ApplicationException(String msg, Object... params) {
        this(Status.BAD_REQUEST, msg, params);
    }

    public ApplicationException(Status status, String msg, Object... params) {
        this(status.getStatusCode(), msg, params);
    }

    public ApplicationException(Integer statusCode, String msg, Object... params) {
        super(msg, MessageFormatter.getThrowableCandidate(params));
        this.params = params;
        this.statusCode = statusCode;
    }

    public Error toError() {
        return new Error(this.getMessage());
    }

    @Override
    public synchronized String getMessage() {
        if (formattedMessage == null) {
            formattedMessage = MessageFormatter.arrayFormat(super.getMessage(), params).getMessage();
        }
        return formattedMessage;
    }

    public Integer getCode() {
        return statusCode;
    }
}

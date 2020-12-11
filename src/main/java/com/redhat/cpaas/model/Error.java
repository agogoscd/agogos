package com.redhat.cpaas.model;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Error {

    @Getter
    @Setter
    String message;

    @Getter
    @Setter
    String docs;

    @Getter
    @Setter
    Integer status = Status.BAD_REQUEST.getStatusCode();

    public Error() {
    }

    public Error(String message) {
        this.message = message;
    }

    public Error(String message, String docs) {
        this.message = message;
        this.docs = docs;
    }
}

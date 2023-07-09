package com.redhat.agogos.interceptors;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Status {

    @Getter
    @Setter
    private Code code = Code.OK;

    @Getter
    @Setter
    private String message;
}
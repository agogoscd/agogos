package com.redhat.agogos.interceptors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterceptorResponse {

    @Getter
    @Setter
    private Map<String, Object> extensions;

    @Getter
    @Setter
    @JsonProperty("continue")
    private boolean continueFlag = true;

    @Getter
    @Setter
    private Status status = new Status();
}

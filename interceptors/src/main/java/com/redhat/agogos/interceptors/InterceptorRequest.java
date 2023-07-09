package com.redhat.agogos.interceptors;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

public class InterceptorRequest {

    public InterceptorRequest() {
    }

    @Getter
    @Setter
    private String body;

    @Getter
    @Setter
    private Map<String, String[]> header;

    @Getter
    @Setter
    private Map<String, Object> extensions;

    @JsonProperty("interceptor_params")
    @Getter
    @Setter
    private Map<String, Object> interceptorParams;

    @Getter
    @Setter
    private TriggerContext context;
}

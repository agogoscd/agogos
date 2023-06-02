package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = JsonDeserializer.None.class)
@RegisterForReflection
public class ComponentHandlerSpec implements KubernetesResource {

    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class HandlerRef implements KubernetesResource {
        @Getter
        @Setter
        private String name;
    }

    @Getter
    @Setter
    private HandlerRef handlerRef = new HandlerRef();

    @Getter
    @Setter
    private Map<String, Object> params = new HashMap<>();

    @Getter
    @Setter
    private Map<String, Object> config = new HashMap<>();
}

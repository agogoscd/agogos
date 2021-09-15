package com.redhat.agogos.v1alpha1;

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
@JsonDeserialize(using = JsonDeserializer.None.class)
@RegisterForReflection
public class ComponentHandlerSpec implements KubernetesResource {

    @ToString
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

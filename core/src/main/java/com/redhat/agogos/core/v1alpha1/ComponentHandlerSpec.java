package com.redhat.agogos.core.v1alpha1;

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
import java.util.Objects;

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

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof HandlerRef)) {
                return false;
            }

            HandlerRef ref = (HandlerRef) obj;

            return Objects.equals(ref.getName(), getName());
        }
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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ComponentHandlerSpec)) {
            return false;
        }

        ComponentHandlerSpec spec = (ComponentHandlerSpec) obj;

        return Objects.equals(spec.getHandlerRef(), getHandlerRef())
                && Objects.equals(spec.getParams(), getParams())
                && Objects.equals(spec.getConfig(), getConfig());
    }
}

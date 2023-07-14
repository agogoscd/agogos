package com.redhat.agogos.core.v1alpha1.triggers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.tekton.triggers.v1beta1.TriggerInterceptor;
import io.fabric8.tekton.triggers.v1beta1.TriggerInterceptorBuilder;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({
        @JsonSubTypes.Type(BuildTriggerEvent.class),
        @JsonSubTypes.Type(CelTriggerEvent.class),
        @JsonSubTypes.Type(GroupTriggerEvent.class),
        @JsonSubTypes.Type(PipelineTriggerEvent.class),
        @JsonSubTypes.Type(ComponentTriggerEvent.class),
        @JsonSubTypes.Type(TimedTriggerEvent.class)
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = JsonDeserializer.None.class)
public interface TriggerEvent extends KubernetesResource {

    TriggerInterceptorBuilder builder = new TriggerInterceptorBuilder();

    /**
     * Converts the CEL expression list into a CEL interceptor by anding ("&&") the expressions.
     *
     * @return a CEL interceptor
     */
    @JsonIgnore
    public default TriggerInterceptor toCel(List<String> expressions) {
        return builder.withNewRef()
                .withName("cel")
                .endRef()
                .withParams()
                .addNewParam("filter", String.join("\n&& ", expressions))
                .build();
    }

    /**
     * Converts the event definition into a list of interceptors.
     *
     * @return List of interceptors
     */
    @JsonIgnore
    public List<TriggerInterceptor> interceptors(Trigger trigger);
}

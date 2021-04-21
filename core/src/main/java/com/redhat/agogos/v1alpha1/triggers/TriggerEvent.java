package com.redhat.agogos.v1alpha1.triggers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
@JsonSubTypes({ //
        @JsonSubTypes.Type(BuildTriggerEvent.class), //
        @JsonSubTypes.Type(CelTriggerEvent.class), //
        @JsonSubTypes.Type(GroupTriggerEvent.class), //
        @JsonSubTypes.Type(PipelineTriggerEvent.class), //
        @JsonSubTypes.Type(ComponentTriggerEvent.class), //
        @JsonSubTypes.Type(TimedTriggerEvent.class) //
})
@JsonDeserialize(using = JsonDeserializer.None.class)
public interface TriggerEvent extends KubernetesResource {

    /**
     * Converts the event definition into CEL expression list.
     *
     * Returns CEL representation of the {@link TriggerEvent} as a list. It can
     * contain one or more expressions.
     * 
     * @return List of CEL expressions
     */
    @JsonIgnore
    public List<String> toCel(Trigger trigger);
}

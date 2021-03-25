package com.redhat.agogos.v1alpha1.triggers;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RegisterForReflection
@JsonDeserialize(using = JsonDeserializer.None.class)
public class TriggerTarget implements KubernetesResource {
    private static final long serialVersionUID = 4518563268155498877L;

    private String apiVersion;

    private String kind;

    private String name;
}

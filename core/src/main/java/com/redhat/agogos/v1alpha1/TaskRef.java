package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = JsonDeserializer.None.class)
@RegisterForReflection
public class TaskRef implements KubernetesResource {
    private static final long serialVersionUID = 5507683698215774978L;

    @Getter
    @Setter
    private String kind;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String url;
}

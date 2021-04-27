package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.ResourceStatus;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@JsonDeserialize(using = JsonDeserializer.None.class)
@RegisterForReflection
public class Status implements KubernetesResource {

    private static final long serialVersionUID = -3677250631346179789L;

    @Getter
    @Setter
    protected String status = String.valueOf(ResourceStatus.New);
    @Getter
    @Setter
    protected String reason;
    @Getter
    @Setter
    protected String lastUpdate;
}

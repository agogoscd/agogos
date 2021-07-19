package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@ToString
@JsonDeserialize(using = JsonDeserializer.None.class)
@RegisterForReflection
public abstract class AgogosResourceStatus implements KubernetesResource {

    private static final long serialVersionUID = -3677250631346179789L;

    @Getter
    @Setter
    protected String status;
    @Getter
    @Setter
    protected String reason;
    @Getter
    @Setter
    protected String lastUpdate;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AgogosResourceStatus)) {
            return false;
        }

        AgogosResourceStatus status = (AgogosResourceStatus) obj;

        if (Objects.equals(status.getStatus(), getStatus()) &&
                Objects.equals(status.getReason(), getReason()) &&
                Objects.equals(status.getLastUpdate(), getLastUpdate())) {
            return true;
        }

        return false;
    }
}
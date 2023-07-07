package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = JsonDeserializer.None.class)
@RegisterForReflection
public abstract class AgogosResourceStatus {

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

        if (Objects.equals(status.getReason(), getReason()) &&
                Objects.equals(status.getLastUpdate(), getLastUpdate())) {
            return true;
        }

        return false;
    }
}

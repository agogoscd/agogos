package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.redhat.agogos.core.ResourceStatus;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class Status extends AgogosResourceStatus {

    @Getter
    @Setter
    protected ResourceStatus status = ResourceStatus.NEW;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Status)) {
            return false;
        }

        if (!super.equals(obj)) {
            return false;
        }

        Status status = (Status) obj;

        if (Objects.equals(status.getStatus(), getStatus())) {
            return true;
        }

        return false;
    }
}

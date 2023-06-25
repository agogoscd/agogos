package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.redhat.agogos.v1alpha1.Component.ComponentSpec;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class ResultableBuildStatus extends ResultableStatus {

    private static final long serialVersionUID = -3677250631346179780L;

    @Getter
    @Setter
    ComponentSpec componentSpec;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ResultableBuildStatus)) {
            return false;
        }

        if (!super.equals(obj)) {
            return false;
        }

        ResultableBuildStatus status = (ResultableBuildStatus) obj;

        return Objects.equals(status.getComponentSpec(), getComponentSpec());
    }
}

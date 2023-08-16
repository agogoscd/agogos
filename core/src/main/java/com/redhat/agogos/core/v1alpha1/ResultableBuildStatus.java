package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.redhat.agogos.core.v1alpha1.Component.ComponentSpec;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class ResultableBuildStatus extends ResultableStatus {

    @Getter
    @Setter
    ComponentSpec componentSpec;

    @Getter
    @Setter
    String output;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ResultableBuildStatus)) {
            return false;
        }

        if (!super.equals(obj)) {
            return false;
        }

        ResultableBuildStatus status = (ResultableBuildStatus) obj;

        return Objects.equals(status.getComponentSpec(), getComponentSpec())
                && Objects.equals(status.getOutput(), getOutput());
    }
}

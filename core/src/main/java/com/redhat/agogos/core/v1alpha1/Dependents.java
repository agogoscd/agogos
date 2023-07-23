package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = JsonDeserializer.None.class)
@RegisterForReflection
public class Dependents {

    @Getter
    @Setter
    private List<String> components = new ArrayList<>();

    @Getter
    @Setter
    private List<String> groups = new ArrayList<>();

    @Getter
    @Setter
    private List<String> pipelines = new ArrayList<>();

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Dependents)) {
            return false;
        }

        Dependents deps = (Dependents) obj;

        return Objects.equals(deps.getComponents(), getComponents())
                && Objects.equals(deps.getGroups(), getGroups())
                && Objects.equals(deps.getPipelines(), getPipelines());
    }
}

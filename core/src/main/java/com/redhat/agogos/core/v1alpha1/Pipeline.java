package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.core.v1alpha1.Pipeline.PipelineSpec;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
@Kind("Pipeline")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Pipeline extends AgogosResource<PipelineSpec, Status> implements Namespaced {

    private static final long serialVersionUID = 4918853237265675286L;

    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class PipelineSpec {
        @Getter
        @Setter
        private List<StageEntry> stages = new ArrayList<>();

        @Getter
        @Setter
        private Dependents dependents = new Dependents();

    }

    @Override
    protected PipelineSpec initSpec() {
        return new PipelineSpec();
    }

    @Override
    protected Status initStatus() {
        return new Status();
    }
}

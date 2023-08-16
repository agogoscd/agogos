package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.core.v1alpha1.Pipeline.PipelineSpec;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        @ToString
        @JsonDeserialize(using = JsonDeserializer.None.class)
        @RegisterForReflection
        public static class StageEntry implements KubernetesResource {
            private static final long serialVersionUID = -7474791004555510761L;

            @Getter
            @Setter
            private StageReference stageRef = new StageReference();

            @Getter
            @Setter
            private List<String> runAfter;

            @Getter
            @Setter
            private Map<Object, Object> config = new HashMap<>();
        }

        @ToString
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonDeserialize(using = JsonDeserializer.None.class)
        @RegisterForReflection
        public static class StageReference implements KubernetesResource {
            private static final long serialVersionUID = -7474791004592310761L;

            @Getter
            @Setter
            private String name;

            @Getter
            @Setter
            private String kind;

            @JsonIgnore // TODO: Double check if this works, added brcause of: Caused by: java.lang.IllegalArgumentException: Found a cyclic reference involving the field stages of type com.redhat.agogos.core.v1alpha1.Pipeline$PipelineSpec.StageEntry
            @JsonBackReference
            @Getter
            @Setter
            public Pipeline pipeline;
        }

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

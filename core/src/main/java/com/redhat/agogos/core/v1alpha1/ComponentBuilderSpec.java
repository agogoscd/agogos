package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.tekton.pipeline.v1beta1.Param;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = JsonDeserializer.None.class)
@RegisterForReflection
public class ComponentBuilderSpec implements KubernetesResource {
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class BuilderRef implements KubernetesResource {
        /**
         * Name of the {@link Builder}.
         */
        @Getter
        @Setter
        private String name;
        /**
         * Namespace of the {@link Builder}.
         */
        @Getter
        @Setter
        private String namespace;

        public BuilderRef() {
            super();
        }

        public BuilderRef(String name) {
            super();
            this.name = name;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BuilderRef)) {
                return false;
            }

            BuilderRef ref = (BuilderRef) obj;

            return Objects.equals(ref.getName(), getName())
                    && Objects.equals(ref.getNamespace(), getNamespace());
        }
    }

    @Getter
    @Setter
    private BuilderRef builderRef = new BuilderRef();

    @Getter
    @Setter
    private List<Param> params = new ArrayList<>();

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ComponentBuilderSpec)) {
            return false;
        }

        ComponentBuilderSpec spec = (ComponentBuilderSpec) obj;

        return Objects.equals(spec.getBuilderRef(), getBuilderRef())
                && Objects.equals(spec.getParams(), getParams());
    }
}

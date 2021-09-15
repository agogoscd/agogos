package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
@JsonDeserialize(using = JsonDeserializer.None.class)
@RegisterForReflection
public class ComponentBuilderSpec implements KubernetesResource {
    @ToString
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
         * Kind of the {@link Builder}.
         */
        @Getter
        @Setter
        private String kind = HasMetadata.getKind(Builder.class);

        /**
         * Version of the {@link Builder}.
         */
        @Getter
        @Setter
        private String version = HasMetadata.getVersion(Builder.class);

        public BuilderRef() {
            super();
        }

        public BuilderRef(String name) {
            super();
            this.name = name;
        }
    }

    @Getter
    @Setter
    private BuilderRef builderRef = new BuilderRef();

    @Getter
    @Setter
    private Map<String, Object> params = new HashMap<>();
}

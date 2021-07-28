package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.ResourceStatus;
import com.redhat.agogos.v1alpha1.Component.ComponentSpec;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
@RegisterForReflection
@Kind("Component")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Component extends AgogosResource<ComponentSpec, Status> implements Namespaced {
    private static final long serialVersionUID = 9122121231081986174L;

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class SourceHandlerRef implements KubernetesResource {
        /**
         * Name of the {@link SourceHandler}.
         */
        @Getter
        @Setter
        private String name;
    }

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

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class SourceSpec implements KubernetesResource {
        /**
         * A reference to select {@link SourceHandler}.
         */
        @Getter
        @Setter
        SourceHandlerRef handlerRef = new SourceHandlerRef();

        /**
         * Configuration passed to the {@link SourceHandler}.
         */
        @Getter
        @Setter
        private Map<Object, Object> data = new HashMap<>();
    }

    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class ComponentSpec implements KubernetesResource {
        private static final long serialVersionUID = -2068477162805635444L;
        @Getter
        @Setter
        private SourceSpec source = new SourceSpec();

        @Getter
        @Setter
        private BuilderRef builderRef = new BuilderRef();

        @Getter
        @Setter
        private Map<Object, Object> data = new HashMap<>();

    }

    public Component() {
        super();
    }

    public Component(String name) {
        super();

        this.getMetadata().setName(name);
    }

    @JsonIgnore
    public boolean isReady() {
        if (ResourceStatus.valueOf(getStatus().getStatus()) == ResourceStatus.Ready) {
            return true;
        }

        return false;
    }

    @Getter
    @Setter
    private ComponentSpec spec = new ComponentSpec();

    @Override
    protected Status initStatus() {
        return new Status();
    }
}

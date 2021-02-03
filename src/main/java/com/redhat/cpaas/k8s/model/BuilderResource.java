package com.redhat.cpaas.k8s.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.cpaas.k8s.model.BuilderResource.BuilderSpec;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@RegisterForReflection
@Kind("Builder")
@Group("cpaas.redhat.com")
@Version("v1alpha1")
public class BuilderResource extends CustomResource<BuilderSpec, Void> implements Namespaced {
    private static final long serialVersionUID = 9122121231081986174L;

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class BuilderSchema implements KubernetesResource {
        private static final long serialVersionUID = -1996106000759225739L;

        @Getter
        @Setter
        private Map<Object, Object> openAPIV3Schema = new HashMap<>();
    }

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class BuilderSpec implements KubernetesResource {
        private static final long serialVersionUID = -3961879962688290544L;

        @Getter
        @Setter
        private String task;
        @Getter
        @Setter
        private BuilderSchema schema = new BuilderSchema();
    }

    public BuilderResource() {
        super();
    }

    @Getter
    @Setter
    private BuilderSpec spec = new BuilderSpec();

}

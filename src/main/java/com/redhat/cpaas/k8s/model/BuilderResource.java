package com.redhat.cpaas.k8s.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.cpaas.model.Builder;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@RegisterForReflection
public class BuilderResource extends CustomResource implements Namespaced {
    public static String KIND = "Builder";

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
        private List<String> types = new ArrayList<>();
        @Getter
        @Setter
        private BuilderSchema schema = new BuilderSchema();
    }

    public BuilderResource() {
        super(KIND);
    }

    public BuilderResource(Builder builder) {
        super(KIND);
        // HashMap<String, String> annotations = new HashMap<>();
        // annotations.put("status", builder.getStatus().toString());

        this.getMetadata().setName(builder.getName());
        // this.getMetadata().setAnnotations(annotations);
        this.getSpec().setTask(builder.getTask());
        this.getSpec().getSchema().setOpenAPIV3Schema(builder.getSchema());
    }

    @Getter
    @Setter
    private BuilderSpec spec = new BuilderSpec();

}

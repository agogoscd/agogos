package com.redhat.cpaas.k8s.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.redhat.cpaas.model.Builder;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;

@RegisterForReflection
public class BuilderResource extends CustomResource implements Namespaced {
    public static String KIND = "Builder";

    private static final long serialVersionUID = 9122121231081986174L;

    public static class BuilderSchema {
        @Getter
        @Setter
        private Map<Object, Object> openAPIV3Schema = new HashMap<>();
    }

    // @JsonIgnoreProperties(value = { "schema" })
    @RegisterForReflection
    public static class BuilderSpec {
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
        this.setKind(KIND);
    }

    public BuilderResource(Builder builder) {
        // HashMap<String, String> annotations = new HashMap<>();
        // annotations.put("status", builder.getStatus().toString());

        this.setKind(KIND);
        this.getMetadata().setName(builder.getName());
        // this.getMetadata().setAnnotations(annotations);
        this.getSpec().setTask(builder.getTask());
        this.getSpec().getSchema().setOpenAPIV3Schema(builder.getSchema());
    }

    @Getter
    private BuilderSpec spec = new BuilderSpec();

}

package com.redhat.cpaas.k8s.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class StageResource extends CustomResource implements Namespaced {

    private static final long serialVersionUID = 1191367775606383094L;
    public static final String KIND = "Stage";

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class StageSpec implements KubernetesResource {

        private static final long serialVersionUID = 3644066384389447653L;

        public static class Schema {
            @Getter
            @Setter
            private Map<Object, Object> openAPIV3Schema = new HashMap<>();
        }

        @Getter
        @Setter
        private String task;

        @Getter
        @Setter
        private Schema schema = new Schema();

    }

    StageResource() {
        super(KIND);
    }

    @Getter
    @Setter
    private StageSpec spec = new StageSpec();
}

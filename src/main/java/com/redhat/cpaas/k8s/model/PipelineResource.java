package com.redhat.cpaas.k8s.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

@ToString
@RegisterForReflection
public class PipelineResource extends CustomResource implements Namespaced {

    public static final String KIND = "Pipeline";
    private static final long serialVersionUID = 4918853237265675286L;

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class PipelineSpec implements KubernetesResource {

        @ToString
        @JsonDeserialize(using = JsonDeserializer.None.class)
        @RegisterForReflection
        public static class StageReference implements KubernetesResource {
            private static final long serialVersionUID = -7474791004592310761L;

            @Getter
            @Setter
            private String name;

            @Getter
            @Setter
            private Map<String, String> config = new HashMap<>();
        }

        private static final long serialVersionUID = 3644066384389447653L;

        @Getter
        @Setter
        private String group;

        @Getter
        @Setter
        private List<StageReference> stages = new ArrayList<>();

    }

    PipelineResource() {
        super(KIND);
    }

    @Getter
    @Setter
    private PipelineSpec spec = new PipelineSpec();
}

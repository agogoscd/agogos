package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.v1alpha1.Pipeline.PipelineSpec;
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
@RegisterForReflection
@Kind("Pipeline")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Pipeline extends AgogosResource<PipelineSpec, Status> implements Namespaced {

    private static final long serialVersionUID = 4918853237265675286L;

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class PipelineSpec implements KubernetesResource {

        @ToString
        @JsonDeserialize(using = JsonDeserializer.None.class)
        @RegisterForReflection
        public static class StageEntry implements KubernetesResource {
            private static final long serialVersionUID = -7474791004592310761L;

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

            @JsonBackReference
            @Getter
            @Setter
            public Pipeline pipeline;
        }

        private static final long serialVersionUID = 3644066384389447653L;

        @Getter
        @Setter
        private String group;

        @Getter
        @Setter
        private List<StageEntry> stages = new ArrayList<>();

    }

    /**
     * <p>
     * Returns object name together with namespace. Useful for logging.
     * </p>
     * 
     * @return String in format: <code>[NAMESPACE]/[NAME]</code>
     */
    @JsonIgnore
    public String getNamespacedName() {
        return this.getMetadata().getNamespace() + "/" + this.getMetadata().getName();
    }

    @Getter
    @Setter
    private PipelineSpec spec = new PipelineSpec();

    @Setter
    @Getter
    private Status status = new Status();
}

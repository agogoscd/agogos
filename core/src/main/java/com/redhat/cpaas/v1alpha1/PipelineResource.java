package com.redhat.cpaas.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.cpaas.v1alpha1.PipelineResource.PipelineSpec;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@RegisterForReflection
@Kind("Pipeline")
@Group("cpaas.redhat.com")
@Version("v1alpha1")
public class PipelineResource extends CustomResource<PipelineSpec, Void> implements Namespaced {

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
            private List<String> runAfter;

            @Getter
            @Setter
            private Map<Object, Object> config = new HashMap<>();
        }

        private static final long serialVersionUID = 3644066384389447653L;

        @Getter
        @Setter
        private String group;

        @Getter
        @Setter
        private List<StageReference> stages = new ArrayList<>();

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
}

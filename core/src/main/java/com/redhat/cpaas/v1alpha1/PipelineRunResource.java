package com.redhat.cpaas.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.cpaas.v1alpha1.PipelineRunResource.RunSpec;
import com.redhat.cpaas.v1alpha1.PipelineRunResource.RunStatus;
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
@Kind("PipelineRun")
@Group("cpaas.redhat.com")
@Version("v1alpha1")
public class PipelineRunResource extends CustomResource<RunSpec, RunStatus> implements Namespaced {
    private static final long serialVersionUID = 6688424087008846788L;

    public enum Status {
        New,
        Initialized,
        Running,
        Passed,
        Failed,
        Aborted;
    }

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class RunStatus implements KubernetesResource {

        private static final long serialVersionUID = -3677250631346179789L;
        @Getter
        @Setter
        private String status = String.valueOf(Status.New);
        @Getter
        @Setter
        private String reason;
        @Getter
        @Setter
        private String lastUpdate;
    }

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class RunSpec implements KubernetesResource {
        private static final long serialVersionUID = -8979203850878721124L;
        @Getter
        @Setter
        private String pipeline;
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
    private RunSpec spec = new RunSpec();

    @Getter
    @Setter
    private RunStatus status = new RunStatus();
}

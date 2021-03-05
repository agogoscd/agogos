package com.redhat.cpaas.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.cpaas.v1alpha1.ComponentBuildResource.BuildSpec;
import com.redhat.cpaas.v1alpha1.ComponentBuildResource.BuildStatus;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@RegisterForReflection
@Kind("ComponentBuild")
@Group("cpaas.redhat.com")
@Version("v1alpha1")
public class ComponentBuildResource extends CustomResource<BuildSpec, BuildStatus> implements Namespaced {
    public enum Status {
        New,
        Initialized,
        Running,
        Passed,
        Failed,
        Aborted;
    }

    private static final long serialVersionUID = 9122121231081986174L;

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class BuildStatus implements KubernetesResource {
        private static final long serialVersionUID = 1554582184774488817L;

        @Getter
        @Setter
        private Map<Object, Object> result = new HashMap<>();
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
    public static class BuildSpec implements KubernetesResource {
        private static final long serialVersionUID = 4609282852879403086L;

        @Getter
        @Setter
        private String component;
    }

    public ComponentBuildResource() {
        super();
    }

    @Getter
    @Setter
    private BuildSpec spec = new BuildSpec();

    @Getter
    @Setter
    private BuildStatus status = new BuildStatus();

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

    /**
     * <p>
     * Get the result of the build.
     * </p>
     * 
     * @return String formatted result
     */
    @JsonIgnore
    public Map<Object, Object> getResult() {
        return this.getStatus().getResult();
    }
}

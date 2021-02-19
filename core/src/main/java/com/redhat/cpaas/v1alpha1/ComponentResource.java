package com.redhat.cpaas.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.cpaas.v1alpha1.ComponentResource.ComponentSpec;
import com.redhat.cpaas.v1alpha1.ComponentResource.ComponentStatus;
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
@Kind("Component")
@Group("cpaas.redhat.com")
@Version("v1alpha1")
public class ComponentResource extends CustomResource<ComponentSpec, ComponentStatus> implements Namespaced {
    public enum Status {
        New,
        Initializing,
        Ready,
        Failed;
    }

    private static final long serialVersionUID = 9122121231081986174L;

    @JsonDeserialize(using = JsonDeserializer.None.class)
    @ToString
    @RegisterForReflection
    public static class ComponentStatus implements KubernetesResource {
        private static final long serialVersionUID = 8090667061734131108L;

        @Getter
        @Setter
        private String status = String.valueOf(Status.New);
        @Getter
        @Setter
        private String reason;
        @Getter
        @Setter
        private String lastUpdate;

        public Status toEnum() {
            return Status.valueOf(status);
        }
    }

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class ComponentSpec implements KubernetesResource {
        private static final long serialVersionUID = -2068477162805635444L;

        @Getter
        @Setter
        private String builder;

        @Getter
        @Setter
        private Map<String, String> data = new HashMap<>();

    }

    public ComponentResource() {
        super();
    }

    @JsonIgnore
    public boolean isReady() {
        if (getStatus().toEnum() == Status.Ready) {
            return true;
        }

        return false;
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

    /**
     * Returns a {@link Map} with the most relevant fields from the
     * {@link ComponentResource}.
     *
     * @return An Immutable {@link Map}
     */
    @JsonIgnore
    public Map<String, Object> toEasyMap() {
        return Map.of("builder", this.getSpec().getBuilder(), "data", this.getSpec().getData(), "name",
                this.getMetadata().getName(), "status", this.getStatus().getStatus());
    }

    @Getter
    @Setter
    private ComponentSpec spec = new ComponentSpec();

    @Setter
    @Getter
    private ComponentStatus status = new ComponentStatus();
}

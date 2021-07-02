package com.redhat.agogos.v1alpha1.triggers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.Status;
import com.redhat.agogos.v1alpha1.triggers.Trigger.TriggerSpec;
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
import java.util.List;

@Getter
@Setter
@ToString
@RegisterForReflection
@Kind("Trigger")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Trigger extends AgogosResource<TriggerSpec, Status> implements Namespaced {

    @Getter
    @Setter
    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class TriggerSpec implements KubernetesResource {

        private static final long serialVersionUID = 6338334242266044209L;

        private List<TriggerEvent> events = new ArrayList<>();

        private TriggerTarget target = new TriggerTarget();
    }

    private static final long serialVersionUID = 127976417696596732L;

    @Getter
    @Setter
    private TriggerSpec spec = new TriggerSpec();

    @Getter
    @Setter
    private Status status = new Status();

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
}

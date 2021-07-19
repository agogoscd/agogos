package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.ResourceStatus;
import com.redhat.agogos.v1alpha1.Component.ComponentSpec;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
@RegisterForReflection
@Kind("Component")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Component extends AgogosResource<ComponentSpec, Status> implements Namespaced {
    private static final long serialVersionUID = 9122121231081986174L;

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class ComponentSpec implements KubernetesResource {
        private static final long serialVersionUID = -2068477162805635444L;

        @Getter
        @Setter
        private Map<String, String> builderRef = new HashMap<>();

        @Getter
        @Setter
        private Map<Object, Object> data = new HashMap<>();

    }

    public Component() {
        super();
    }

    public Component(String name) {
        super();

        this.getMetadata().setName(name);
    }

    @JsonIgnore
    public boolean isReady() {
        if (ResourceStatus.valueOf(getStatus().getStatus()) == ResourceStatus.Ready) {
            return true;
        }

        return false;
    }

    @Getter
    @Setter
    private ComponentSpec spec = new ComponentSpec();

    @Override
    protected Status initStatus() {
        return new Status();
    }
}
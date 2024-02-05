package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.core.ResourceStatus;
import com.redhat.agogos.core.v1alpha1.Component.ComponentSpec;
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
import java.util.Objects;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
@Kind("Component")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Component extends AgogosResource<ComponentSpec, Status> implements Namespaced {
    private static final long serialVersionUID = 9122121231081986174L;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class ComponentSpec {

        @Getter
        @Setter
        private List<StageEntry> pre = new ArrayList<>();

        @Getter
        @Setter
        private List<StageEntry> post = new ArrayList<>();

        @Getter
        @Setter
        private ComponentBuilderSpec build = new ComponentBuilderSpec();

        @Getter
        @Setter
        private Dependents dependents = new Dependents();

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ComponentSpec)) {
                return false;
            }

            ComponentSpec spec = (ComponentSpec) obj;

            return Objects.equals(spec.getPre(), getPre())
                    && Objects.equals(spec.getPost(), getPost())
                    && Objects.equals(spec.getBuild(), getBuild())
                    && Objects.equals(spec.getDependents(), getDependents());
        }
    }

    public Component() {
        super();

        this.status = new Status();
    }

    public Component(String name) {
        this();

        this.getMetadata().setName(name);
    }

    @JsonIgnore
    public boolean isReady() {
        if (getStatus().getStatus() == ResourceStatus.READY) {
            return true;
        }

        return false;
    }

    @Override
    protected ComponentSpec initSpec() {
        return new ComponentSpec();
    }
}

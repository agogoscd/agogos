package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.core.ResourceStatus;
import com.redhat.agogos.core.v1alpha1.Group.GroupSpec;
import io.fabric8.kubernetes.api.model.Namespaced;
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
@Kind("Group")
@io.fabric8.kubernetes.model.annotation.Group("agogos.redhat.com")
@Version("v1alpha1")
public class Group extends AgogosResource<GroupSpec, Status> implements Namespaced {
    private static final long serialVersionUID = -7092342726608099745L;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class GroupSpec {

        @Getter
        @Setter
        private List<String> components = new ArrayList<>();

        @Getter
        @Setter
        private List<String> groups = new ArrayList<>();

        @Getter
        @Setter
        private List<String> pipelines = new ArrayList<>();

        @Getter
        @Setter
        private Dependents dependents = new Dependents();

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof GroupSpec)) {
                return false;
            }

            GroupSpec spec = (GroupSpec) obj;

            return Objects.equals(spec.getComponents(), getComponents())
                    && Objects.equals(spec.getGroups(), getGroups())
                    && Objects.equals(spec.getPipelines(), getPipelines())
                    && Objects.equals(spec.getDependents(), getDependents());
        }
    }

    public Group() {
        super();

        this.status = new Status();
    }

    public Group(String name) {
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
    protected GroupSpec initSpec() {
        return new GroupSpec();
    }
}

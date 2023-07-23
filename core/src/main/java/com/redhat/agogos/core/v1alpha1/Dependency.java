package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Dependency.DependencySpec;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
@Kind("Dependency")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Dependency extends AgogosResource<DependencySpec, Void> implements Namespaced {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class DependencySpec {
        public DependencySpec() {
        }

        @Getter
        @Setter
        private String name;

        @Getter
        @Setter
        private String instance;

        @Getter
        @Setter
        private Resource resource;

        @Getter
        @Setter
        private String group;

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DependencySpec)) {
                return false;
            }

            DependencySpec spec = (DependencySpec) obj;

            return Objects.equals(spec.getName(), getName())
                    && Objects.equals(spec.getInstance(), getInstance())
                    && Objects.equals(spec.getResource(), getResource())
                    && Objects.equals(spec.getGroup(), getGroup());
        }
    }

    public Dependency() {
        super();
    }

    @Override
    protected DependencySpec initSpec() {
        return new DependencySpec();
    }
}

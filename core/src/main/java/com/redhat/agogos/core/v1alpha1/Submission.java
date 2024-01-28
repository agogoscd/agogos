package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Submission.SubmissionSpec;
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
@Kind("Submission")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Submission extends AgogosResource<SubmissionSpec, Void> implements Namespaced {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class SubmissionSpec {
        public SubmissionSpec() {
        }

        @Getter
        @Setter
        private String name;

        @Getter
        @Setter
        private String generatedName;

        @Getter
        @Setter
        private Resource resource;

        @Getter
        @Setter
        private String instance;

        @Getter
        @Setter
        private String group;

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SubmissionSpec)) {
                return false;
            }

            SubmissionSpec spec = (SubmissionSpec) obj;

            return Objects.equals(spec.getName(), getName())
                    && Objects.equals(spec.getGeneratedName(), getGeneratedName())
                    && Objects.equals(spec.getResource(), getResource())
                    && Objects.equals(spec.getInstance(), getInstance())
                    && Objects.equals(spec.getGroup(), getGroup());
        }
    }

    public Submission() {
        super();
    }

    @Override
    protected SubmissionSpec initSpec() {
        return new SubmissionSpec();
    }
}

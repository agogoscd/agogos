package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.core.v1alpha1.Run.RunSpec;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
@Kind("Run")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Run extends AgogosResource<RunSpec, ResultableStatus> implements Namespaced {
    private static final long serialVersionUID = 6688424087008846788L;

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class RunSpec {

        @Getter
        @Setter
        private String pipeline;
    }

    @Override
    protected RunSpec initSpec() {
        return new RunSpec();
    }

    @Override
    protected ResultableStatus initStatus() {
        return new ResultableStatus();
    }
}

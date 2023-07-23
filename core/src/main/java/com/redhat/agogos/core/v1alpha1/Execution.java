package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.core.v1alpha1.Execution.ExecutionSpec;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
@Kind("Execution")
@io.fabric8.kubernetes.model.annotation.Group("agogos.redhat.com")
@Version("v1alpha1")
public class Execution extends AgogosResource<ExecutionSpec, ResultableStatus> implements Namespaced {
    private static final long serialVersionUID = -7092342726608099999L;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class ExecutionInfo {
        @Getter
        @Setter
        private String name;

        @Getter
        @Setter
        private ResultableStatus status = new ResultableStatus();
    }

    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class ExecutionSpec {

        @Getter
        @Setter
        private String group;

        @Getter
        @Setter
        private HashMap<String, ExecutionInfo> components = new HashMap<String, ExecutionInfo>();

        @Getter
        @Setter
        private HashMap<String, ExecutionInfo> groups = new HashMap<String, ExecutionInfo>();

        @Getter
        @Setter
        private HashMap<String, ExecutionInfo> pipelines = new HashMap<String, ExecutionInfo>();
    }

    @Override
    protected ExecutionSpec initSpec() {
        return new ExecutionSpec();
    }

    @Override
    protected ResultableStatus initStatus() {
        return new ResultableStatus();
    }
}

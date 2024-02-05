package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.core.v1alpha1.Execution.ExecutionSpec;
import com.redhat.agogos.core.v1alpha1.Execution.ExecutionStatus;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
@Kind("Execution")
@io.fabric8.kubernetes.model.annotation.Group("agogos.redhat.com")
@Version("v1alpha1")
public class Execution extends AgogosResource<ExecutionSpec, ExecutionStatus> implements Namespaced {
    private static final long serialVersionUID = -7092342726608099999L;

    public Execution() {
        super();

        this.status = new ExecutionStatus();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class ExecutionInfo {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class ExecutionComponentInfo extends ExecutionInfo {
        public ExecutionComponentInfo() {
        }

        public ExecutionComponentInfo(String component) {
            this.component = component;
        }

        @Getter
        @Setter
        private String component;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class ExecutionGroupInfo extends ExecutionInfo {
        public ExecutionGroupInfo() {
        }

        public ExecutionGroupInfo(String group) {
            this.group = group;
        }

        @Getter
        @Setter
        private String group;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class ExecutionPipelineInfo extends ExecutionInfo {
        public ExecutionPipelineInfo() {
        }

        public ExecutionPipelineInfo(String pipeline) {
            this.pipeline = pipeline;
        }

        @Getter
        @Setter
        private String pipeline;
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
        private Map<String, ExecutionComponentInfo> builds = new LinkedHashMap<String, ExecutionComponentInfo>();

        @Getter
        @Setter
        private Map<String, ExecutionGroupInfo> executions = new LinkedHashMap<String, ExecutionGroupInfo>();

        @Getter
        @Setter
        private Map<String, ExecutionPipelineInfo> runs = new LinkedHashMap<String, ExecutionPipelineInfo>();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class ExecutionInfoStatus {

        @Getter
        @Setter
        private ResultableStatus status = new ResultableStatus();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class ExecutionStatus extends ResultableStatus {
        @Getter
        @Setter
        private Map<String, ExecutionInfoStatus> builds = new LinkedHashMap<String, ExecutionInfoStatus>();

        @Getter
        @Setter
        private Map<String, ExecutionInfoStatus> executions = new LinkedHashMap<String, ExecutionInfoStatus>();

        @Getter
        @Setter
        private Map<String, ExecutionInfoStatus> runs = new LinkedHashMap<String, ExecutionInfoStatus>();
    }

    @Override
    protected ExecutionSpec initSpec() {
        return new ExecutionSpec();
    }
}

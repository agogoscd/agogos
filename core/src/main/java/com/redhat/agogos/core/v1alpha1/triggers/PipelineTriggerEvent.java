package com.redhat.agogos.core.v1alpha1.triggers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.redhat.agogos.core.CloudEventHelper;
import com.redhat.agogos.core.PipelineRunState;
import com.redhat.agogos.core.v1alpha1.Run;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@ToString
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("pipeline")
public class PipelineTriggerEvent implements TriggerEvent {
    private static final long serialVersionUID = -4379042700549506262L;

    private String name;

    @Override
    public List<String> toCel(Trigger trigger) {
        return Arrays.asList( //
                String.format("header.match('ce-type', '%s')",
                        CloudEventHelper.type(Run.class, PipelineRunState.SUCCEEDED)), //
                String.format("body.pipeline.metadata.name == '%s'", name) //
        );
    }
}

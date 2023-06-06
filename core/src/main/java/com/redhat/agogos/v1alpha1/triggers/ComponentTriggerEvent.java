package com.redhat.agogos.v1alpha1.triggers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.redhat.agogos.CloudEventHelper;
import com.redhat.agogos.PipelineRunState;
import com.redhat.agogos.v1alpha1.Build;
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
@JsonTypeName("component")
public class ComponentTriggerEvent implements TriggerEvent {
    private static final long serialVersionUID = -8379042700549506262L;

    private String name;

    @Override
    public List<String> toCel(Trigger trigger) {
        return Arrays.asList(
                String.format("header.match('ce-type', '%s')",
                        CloudEventHelper.type(Build.class, PipelineRunState.SUCCEEDED)),
                String.format("body.component.metadata.name == '%s'", name));
    }
}

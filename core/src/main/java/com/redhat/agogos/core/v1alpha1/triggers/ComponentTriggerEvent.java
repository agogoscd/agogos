package com.redhat.agogos.core.v1alpha1.triggers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.redhat.agogos.core.PipelineRunState;
import com.redhat.agogos.core.eventing.CloudEventHelper;
import com.redhat.agogos.core.v1alpha1.Build;
import io.fabric8.tekton.triggers.v1beta1.TriggerInterceptor;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
    public List<TriggerInterceptor> interceptors(Trigger trigger) {
        List<String> expressions = List.of(
                String.format("header.match('ce-type', '%s')",
                        CloudEventHelper.type(Build.class, PipelineRunState.SUCCEEDED)),
                String.format("body.component.metadata.name == '%s'", name));
        return List.of(toCel(expressions));
    }
}

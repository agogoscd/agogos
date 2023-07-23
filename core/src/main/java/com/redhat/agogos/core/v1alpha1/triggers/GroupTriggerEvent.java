package com.redhat.agogos.core.v1alpha1.triggers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.PipelineRunState;
import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.eventing.CloudEventHelper;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Group;
import com.redhat.agogos.core.v1alpha1.Run;
import io.fabric8.tekton.triggers.v1beta1.TriggerInterceptor;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.inject.spi.CDI;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeName("group")
public class GroupTriggerEvent implements TriggerEvent {
    private static final long serialVersionUID = -2379042700549506262L;

    private String name;

    @Override
    public List<TriggerInterceptor> interceptors(Trigger trigger) {
        List<String> expressions = List.of(
                String.format("(header.match('ce-type', '%s') || header.match('ce-type', '%s'))",
                        CloudEventHelper.type(Build.class, PipelineRunState.SUCCEEDED),
                        CloudEventHelper.type(Run.class, PipelineRunState.SUCCEEDED)),
                generateItemsCelExpression(trigger));

        TriggerInterceptor groupExecuteInterceptor = builder
                .withNewRef()
                .withName("group-execute")
                .endRef()
                .withParams()
                .addNewParam("namespace", trigger.getMetadata().getNamespace())
                .build();
        return List.of(toCel(expressions), groupExecuteInterceptor);
    }

    private String generateItemsCelExpression(Trigger trigger) {
        KubernetesFacade kubernetesFacade = CDI.current().select(KubernetesFacade.class).get();
        Group group = kubernetesFacade.get(Group.class, trigger.getMetadata().getNamespace(), name);
        if (group == null) {
            throw new ApplicationException("Group '{}' could not be found", name);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        List<String> items = group.getSpec().getComponents().stream()
                .map(c -> String.format("body.component.metadata.name == '%s'", c))
                .collect(Collectors.toList());
        items.addAll(group.getSpec().getPipelines().stream()
                .map(c -> String.format("body.pipeline.metadata.name == '%s'", c))
                .collect(Collectors.toList()));

        sb.append(String.join(" || ", items));
        sb.append(")");
        return sb.toString();
    }
}

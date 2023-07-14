package com.redhat.agogos.core.v1alpha1.triggers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.redhat.agogos.core.CloudEventHelper;
import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.PipelineRunState;
import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Group;
import io.fabric8.tekton.triggers.v1beta1.TriggerInterceptor;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.inject.spi.CDI;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

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

        KubernetesFacade kubernetesFacade = CDI.current().select(KubernetesFacade.class).get();

        // Fetch the Group information
        Group componentGroup = kubernetesFacade.get(Group.class, trigger.getMetadata().getNamespace(), name);
        if (componentGroup == null) {
            throw new ApplicationException("ComponentGroup '{}' could not be found", name);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n&&\n");
        sb.append("sets.contains([\n'");
        sb.append(String.join("',\n'", componentGroup.getSpec().getComponents()));
        sb.append("'],\n[ body.component.metadata.name ]\n)");

        List<String> expressions = List.of(
                String.format("header.match('ce-type', '%s')",
                        CloudEventHelper.type(Build.class, PipelineRunState.SUCCEEDED)),
                sb.toString());
        return List.of(toCel(expressions));
    }

}

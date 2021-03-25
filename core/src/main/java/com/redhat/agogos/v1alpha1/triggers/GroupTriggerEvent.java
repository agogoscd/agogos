package com.redhat.agogos.v1alpha1.triggers;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.redhat.cpaas.errors.ApplicationException;
import com.redhat.cpaas.k8s.client.ComponentGroupResourceClient;
import com.redhat.cpaas.v1alpha1.ComponentGroupResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.enterprise.inject.spi.CDI;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RegisterForReflection
@JsonTypeName("group")
public class GroupTriggerEvent implements TriggerEvent {
    private static final long serialVersionUID = -2379042700549506262L;

    private String name;
    private String type = "com.redhat.agogos.event.componentbuild.success.v1alpha1";

    @Override
    public List<String> toCel(Trigger trigger) {
        // Obtain component group client so we can get information about
        // the referenced Group
        ComponentGroupResourceClient componentGroupClient = CDI.current().select(ComponentGroupResourceClient.class)
                .get();

        // Fetch the Group information
        ComponentGroupResource componentGroup = componentGroupClient.getByName(name);

        // TODO: This should be part of the validation webhook
        // But it doesn't hurt to have it here as well
        if (componentGroup == null) {
            throw new ApplicationException("ComponentGroup '{}' could not be found", name);
        }

        StringBuilder builder = new StringBuilder();

        // Build the CEL expression that compares the component
        for (Iterator<String> iter = componentGroup.getSpec().getComponents().iterator(); iter.hasNext();) {
            builder.append(String.format("body.component.metadata.name == '%s'", iter.next()));

            if (iter.hasNext()) {
                builder.append(String.format(" || "));
            }
        }

        return Arrays.asList( //
                String.format("header.match('ce-type', '%s')", type), //
                builder.toString()//
        );
    }

}

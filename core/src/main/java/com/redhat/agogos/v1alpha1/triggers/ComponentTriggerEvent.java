package com.redhat.agogos.v1alpha1.triggers;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RegisterForReflection
@JsonTypeName("component")
public class ComponentTriggerEvent implements TriggerEvent {
    private static final long serialVersionUID = -8379042700549506262L;

    private String name;
    private String type = "com.redhat.agogos.event.componentbuild.success.v1alpha1";

    @Override
    public List<String> toCel(Trigger trigger) {
        return Arrays.asList( //
                String.format("header.match('ce-type', '%s')", type), //
                String.format("body.component.metadata.name == '%s'", name) //
        );
    }
}

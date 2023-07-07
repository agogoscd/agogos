package com.redhat.agogos.core.v1alpha1.triggers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
@JsonTypeName("cel")
public class CelTriggerEvent implements TriggerEvent {
    private static final long serialVersionUID = -8379042700549506262L;

    private String filter;

    @Override
    public List<String> toCel(Trigger trigger) {
        return Arrays.asList(filter);
    }

}

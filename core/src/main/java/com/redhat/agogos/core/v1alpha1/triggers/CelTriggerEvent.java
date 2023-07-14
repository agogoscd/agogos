package com.redhat.agogos.core.v1alpha1.triggers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
@JsonTypeName("cel")
public class CelTriggerEvent implements TriggerEvent {
    private static final long serialVersionUID = -8379042700549506262L;

    private String filter;

    @Override
    public List<TriggerInterceptor> interceptors(Trigger trigger) {
        return List.of(toCel(List.of(filter)));
    }

}

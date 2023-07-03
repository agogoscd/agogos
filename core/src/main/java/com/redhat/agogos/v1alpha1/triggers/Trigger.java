package com.redhat.agogos.v1alpha1.triggers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.Status;
import com.redhat.agogos.v1alpha1.triggers.Trigger.TriggerSpec;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
@Kind("Trigger")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Trigger extends AgogosResource<TriggerSpec, Status> implements Namespaced {

    @Getter
    @Setter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class TriggerSpec {

        private List<TriggerEvent> events = new ArrayList<>();

        private TriggerTarget target = new TriggerTarget();
    }

    private static final long serialVersionUID = 127976417696596732L;

    @Override
    protected TriggerSpec initSpec() {
        return new TriggerSpec();
    }

    @Override
    protected Status initStatus() {
        return new Status();
    }
}

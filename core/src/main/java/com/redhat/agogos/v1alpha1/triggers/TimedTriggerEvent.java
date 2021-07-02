package com.redhat.agogos.v1alpha1.triggers;

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
@JsonTypeName("timed")
public class TimedTriggerEvent implements TriggerEvent {
    private static final long serialVersionUID = -8379042711549506262L;

    private String cron;

    @Override
    public List<String> toCel(Trigger trigger) {
        return Arrays.asList();
    }
}

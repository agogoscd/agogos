package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@JsonDeserialize(using = JsonDeserializer.None.class)
@RegisterForReflection
public class RunnableStatus extends Status {

    private static final long serialVersionUID = -3677250631346179789L;

    @Getter
    @Setter
    private Map<?, ?> result;
}

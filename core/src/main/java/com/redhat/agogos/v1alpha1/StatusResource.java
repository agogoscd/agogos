package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.Status;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@JsonDeserialize(using = JsonDeserializer.None.class)
@RegisterForReflection
public class StatusResource implements KubernetesResource {

    private static final long serialVersionUID = -3677250631346179789L;

    @Getter
    @Setter
    private Map<?, ?> result;
    @Getter
    @Setter
    private String status = String.valueOf(Status.New);
    @Getter
    @Setter
    private String reason;
    @Getter
    @Setter
    private String lastUpdate;
}

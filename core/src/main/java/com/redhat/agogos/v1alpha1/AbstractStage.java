package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.v1alpha1.AbstractStage.StageSpec;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@RegisterForReflection
public abstract class AbstractStage extends AgogosResource<StageSpec, Status> {

    public enum Phase {
        BUILD,
        TEST,
        DELIVERY;
    }

    private static final long serialVersionUID = 7447807439691538160L;

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class StageSchema implements KubernetesResource {
        private static final long serialVersionUID = 5507683698215774978L;

        @Getter
        @Setter
        private Map<Object, Object> openAPIV3Schema = new HashMap<>();
    }

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class StageSpec implements KubernetesResource {

        private static final long serialVersionUID = 3644066384389447653L;

        @Getter
        @Setter
        private Map<String, String> taskRef = new HashMap<>();

        @Getter
        @Setter
        private StageSchema schema = new StageSchema();

    }

    @Override
    protected Status initStatus() {
        return new Status();
    }

    @Getter
    @Setter
    private StageSpec spec = new StageSpec();
}

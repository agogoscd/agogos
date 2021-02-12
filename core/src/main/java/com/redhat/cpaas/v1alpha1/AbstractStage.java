package com.redhat.cpaas.v1alpha1;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.cpaas.v1alpha1.AbstractStage.StageSpec;
import com.redhat.cpaas.v1alpha1.AbstractStage.StageStatus;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@RegisterForReflection
public abstract class AbstractStage extends CustomResource<StageSpec, StageStatus> {

    public enum Phase {
        BUILD, TEST, DELIVERY;
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

    @JsonDeserialize(using = JsonDeserializer.None.class)
    @ToString
    @RegisterForReflection
    public static class StageStatus implements KubernetesResource {
        private static final long serialVersionUID = 8090667061734131108L;

        @Getter
        @Setter
        private String status;
        @Getter
        @Setter
        private String reason;
        @Getter
        @Setter
        private String lastUpdate;
    }

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class StageSpec implements KubernetesResource {

        private static final long serialVersionUID = 3644066384389447653L;

        @Getter
        @Setter
        private String task;

        @Getter
        @Setter
        private StageSchema schema = new StageSchema();

    }

    @Getter
    @Setter
    private StageSpec spec = new StageSpec();
}

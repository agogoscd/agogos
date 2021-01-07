package com.redhat.cpaas.k8s.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@RegisterForReflection
public class BuildResource extends CustomResource implements Namespaced {
    public static String KIND = "Build";

    public enum Status {
        New, Initialized, Running, Passed, Failed, Aborted;
    }

    private static final long serialVersionUID = 9122121231081986174L;

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class BuildStatus implements KubernetesResource {
        private static final long serialVersionUID = 1554582184774488817L;

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

    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class BuildSpec implements KubernetesResource {
        private static final long serialVersionUID = 4609282852879403086L;

        @Getter
        @Setter
        private String component;
    }

    public BuildResource() {
        this.setKind(KIND);
    }

    @Getter
    @Setter
    private BuildSpec spec = new BuildSpec();

    @Getter
    @Setter
    private BuildStatus status = new BuildStatus();
}

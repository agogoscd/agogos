package com.redhat.cpaas.k8s.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
    @RegisterForReflection
    public static class BuildStatus {
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
    @RegisterForReflection
    public static class BuildSpec {
        @Getter
        @Setter
        private String component;
    }

    public BuildResource() {
        this.setKind(KIND);
    }

    @Getter
    private BuildSpec spec = new BuildSpec();

    @Getter
    private BuildStatus status = new BuildStatus();
}

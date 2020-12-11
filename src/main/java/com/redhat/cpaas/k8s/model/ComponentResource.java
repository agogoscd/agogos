package com.redhat.cpaas.k8s.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.redhat.cpaas.model.Component;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@RegisterForReflection
public class ComponentResource extends CustomResource implements Namespaced {
    public static String KIND = "Component";

    public enum Status {
        New, Initializing, Ready, Failed;
    }

    private static final long serialVersionUID = 9122121231081986174L;

    @ToString
    @RegisterForReflection
    public static class ComponentStatus {
        @Getter
        @Setter
        private String status = String.valueOf(Status.New);
        @Getter
        @Setter
        private String reason;
        @Getter
        @Setter
        private String lastUpdate;

        public Status toEnum() {
            return Status.valueOf(status);
        }
    }

    @ToString
    @JsonIgnoreProperties(ignoreUnknown = true)
    @RegisterForReflection
    public static class ComponentSpec {
        @Getter
        @Setter
        private String builder;

        // @Getter
        // @Setter
        // private List<ComponentBuild> builds = new ArrayList<>();

        @Getter
        @Setter
        private Map<String, String> data = new HashMap<>();

    }

    // @ToString
    // public static class ComponentBuildSpec {
    // @Getter
    // @Setter
    // private String builder;

    // }

    // @ToString
    // public static class ComponentBuild {

    // // @Getter
    // // private ComponentBuildSpec spec = new ComponentBuildSpec();

    // @Getter
    // @Setter
    // private String builder;

    // @Getter
    // @Setter
    // private Map<String, String> data = new HashMap<>();

    // public ComponentBuild() {

    // }

    // public ComponentBuild(Build build) {
    // this.builder = build.getBuilder();
    // this.data.putAll(build.getData());
    // }

    // }

    public ComponentResource() {
        this.setKind(KIND);
    }

    public ComponentResource(Component component) {
        this.setKind(KIND);
        this.getMetadata().setName(component.getName());
        this.getSpec().getData().putAll(component.getData());
        this.getSpec().setBuilder(component.getBuilder());

        // for (Build build : component.getBuilds()) {
        // this.getSpec().getBuilds().add(new ComponentBuild(build));
        // }
    }

    public boolean isReady() {
        if (getStatus().toEnum() == Status.Ready) {
            return true;
        }

        return false;
    }

    @Getter
    private ComponentSpec spec = new ComponentSpec();

    @Getter
    private ComponentStatus status = new ComponentStatus();
}

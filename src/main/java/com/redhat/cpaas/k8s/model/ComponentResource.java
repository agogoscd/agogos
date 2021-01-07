package com.redhat.cpaas.k8s.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.cpaas.model.Component;

import io.fabric8.kubernetes.api.model.KubernetesResource;
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

    @JsonDeserialize(using = JsonDeserializer.None.class)
    @ToString
    @RegisterForReflection
    public static class ComponentStatus implements KubernetesResource {
        private static final long serialVersionUID = 8090667061734131108L;

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
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @RegisterForReflection
    public static class ComponentSpec implements KubernetesResource {
        private static final long serialVersionUID = -2068477162805635444L;

        @Getter
        @Setter
        private String builder;

        @Getter
        @Setter
        private Map<String, String> data = new HashMap<>();

    }

    public ComponentResource() {
        super(KIND);
    }

    public ComponentResource(Component component) {
        super(KIND);

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
    @Setter
    private ComponentSpec spec = new ComponentSpec();

    @Setter
    @Getter
    private ComponentStatus status = new ComponentStatus();
}

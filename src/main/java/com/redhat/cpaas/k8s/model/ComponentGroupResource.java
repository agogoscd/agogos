package com.redhat.cpaas.k8s.model;

import java.util.ArrayList;
import java.util.List;

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
public class ComponentGroupResource extends CustomResource implements Namespaced {
    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class ComponentGroupResourceSpec implements KubernetesResource {
        private static final long serialVersionUID = 4609282852879403086L;

        @Getter
        @Setter
        private List<String> components = new ArrayList<>();
    }

    private static final long serialVersionUID = -7092342726608099745L;

    public static String KIND = "Group";

    public ComponentGroupResource() {
        this.setKind(KIND);
    }

    @Getter
    @Setter
    private ComponentGroupResourceSpec spec = new ComponentGroupResourceSpec();
}

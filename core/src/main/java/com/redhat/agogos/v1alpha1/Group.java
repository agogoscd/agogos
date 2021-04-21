package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.v1alpha1.Group.ComponentGroupResourceSpec;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@RegisterForReflection
@Kind("Group")
@io.fabric8.kubernetes.model.annotation.Group("agogos.redhat.com")
@Version("v1alpha1")
public class Group extends AgogosResource<ComponentGroupResourceSpec, Void> implements Namespaced {
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

    @Getter
    @Setter
    private ComponentGroupResourceSpec spec = new ComponentGroupResourceSpec();
}

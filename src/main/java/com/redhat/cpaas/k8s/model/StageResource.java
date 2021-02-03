package com.redhat.cpaas.k8s.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.cpaas.k8s.model.StageResource.StageSpec;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@RegisterForReflection
@Kind("Stage")
@Group("cpaas.redhat.com")
@Version("v1alpha1")
public class StageResource extends CustomResource<StageSpec, Void> implements Namespaced {

    private static final long serialVersionUID = 1191367775606383094L;

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class StageSpec implements KubernetesResource {

        private static final long serialVersionUID = 3644066384389447653L;

        public static class Schema {
            @Getter
            @Setter
            private Map<Object, Object> openAPIV3Schema = new HashMap<>();
        }

        @Getter
        @Setter
        private String task;

        @Getter
        @Setter
        private Schema schema = new Schema();

    }

    @Getter
    @Setter
    private StageSpec spec = new StageSpec();
}

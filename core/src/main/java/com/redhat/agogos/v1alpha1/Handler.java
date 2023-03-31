package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.v1alpha1.Handler.HandlerSpec;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ToString
@RegisterForReflection
@Kind("Handler")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Handler extends AgogosResource<HandlerSpec, Void> implements Namespaced {
    private static final long serialVersionUID = 9122121231081986174L;

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class HandlerSchema implements KubernetesResource {
        private static final long serialVersionUID = 5507683698215774978L;

        @Getter
        @Setter
        private Map<Object, Object> openAPIV3Schema = new HashMap<>();
    }

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class HandlerSpec implements KubernetesResource {
        private static final long serialVersionUID = -2068477162805635444L;

        /**
         * <p>
         * {@link TaskRef} providing the implementation for the {@link Handler}.
         * </p>
         */
        @Getter
        @Setter
        private TaskRef taskRef = new TaskRef();

        @Getter
        @Setter
        private HandlerSchema schema = new HandlerSchema();

        @Getter
        @Setter
        private List<WorkspaceMapping> workspaces = new ArrayList<>();
    }

    public Handler() {
        super();
    }

    public Handler(String name) {
        super();

        this.getMetadata().setName(name);
    }

    @Getter
    @Setter
    private HandlerSpec spec = new HandlerSpec();

}
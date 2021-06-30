package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.v1alpha1.SourceHandler.SourceHandlerSpec;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * Cluster scoped resource to register handlers for fetching source code for different SCM.
 * </p>
 */
@ToString
@RegisterForReflection
@Kind("SourceHandler")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class SourceHandler extends AgogosResource<SourceHandlerSpec, Void> {
    private static final long serialVersionUID = 9122121231081986174L;

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class SourceHandlerSchema implements KubernetesResource {
        private static final long serialVersionUID = 5507683698215774978L;

        @Getter
        @Setter
        private Map<Object, Object> openAPIV3Schema = new HashMap<>();
    }

    @ToString
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class SourceHandlerSpec implements KubernetesResource {
        private static final long serialVersionUID = -2068477162805635444L;

        /**
         * <p>
         * {@link TaskRef} providing the implementation for the {@link SourceHandler}.
         * </p>
         */
        @Getter
        @Setter
        private TaskRef taskRef = new TaskRef();

        @Getter
        @Setter
        private SourceHandlerSchema schema = new SourceHandlerSchema();

    }

    public SourceHandler() {
        super();
    }

    public SourceHandler(String name) {
        super();

        this.getMetadata().setName(name);
    }

    @Getter
    @Setter
    private SourceHandlerSpec spec = new SourceHandlerSpec();

}

package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.core.v1alpha1.Handler.HandlerSpec;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.fabric8.tekton.pipeline.v1beta1.TaskRef;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
@Kind("Handler")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Handler extends AgogosResource<HandlerSpec, Void> implements Namespaced {
    private static final long serialVersionUID = 9122121231081986174L;

    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class HandlerSchema {

        @Getter
        @Setter
        private Map<Object, Object> openAPIV3Schema = new HashMap<>();
    }

    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class HandlerSpec {

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

    @Override
    protected HandlerSpec initSpec() {
        return new HandlerSpec();
    }
}

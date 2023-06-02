package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.redhat.agogos.v1alpha1.Builder.BuilderSpec;
import io.fabric8.kubernetes.api.model.KubernetesResource;
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

@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Kind("Builder")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class Builder extends AgogosResource<BuilderSpec, Status> {
    private static final long serialVersionUID = 1184222810180288956L;

    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class BuilderSchema implements KubernetesResource {
        private static final long serialVersionUID = 5507683698215774978L;

        @Getter
        @Setter
        private Map<Object, Object> openAPIV3Schema = new HashMap<>();
    }

    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = JsonDeserializer.None.class)
    @RegisterForReflection
    public static class BuilderSpec implements KubernetesResource {

        private static final long serialVersionUID = 3644066384389447653L;

        @Getter
        @Setter
        private TaskRef taskRef = new TaskRef();

        @Getter
        @Setter
        private BuilderSchema schema = new BuilderSchema();

        @Getter
        @Setter
        private List<WorkspaceMapping> workspaces = new ArrayList<>();

    }

    public Builder() {

    }

    public Builder(String name) {
        this.getMetadata().setName(name);
    }

    @Override
    protected Status initStatus() {
        return new Status();
    }

    @Getter
    @Setter
    private BuilderSpec spec = new BuilderSpec();

}

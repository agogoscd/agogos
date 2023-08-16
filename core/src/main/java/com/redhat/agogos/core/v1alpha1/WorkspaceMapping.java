package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(using = JsonDeserializer.None.class)
@RegisterForReflection
public class WorkspaceMapping implements KubernetesResource {
    private static final long serialVersionUID = 5507683698215774978L;

    /**
     * Name of the main workspace used in Agogos in generated Tekton Pipelines.
     */
    public static final String MAIN_WORKSPACE_NAME = "ws";

    /**
     * <p>
     * Name of the target workspace (declared in the Task).
     * </p>
     */
    @Getter
    @Setter
    private String name;

    /**
     * <p>
     * Subpath on the source workspace that should be exposed as the target workspace.
     * By default root of the workspace is exposed.
     * </p>
     */
    @Getter
    @Setter
    private String subPath;
}

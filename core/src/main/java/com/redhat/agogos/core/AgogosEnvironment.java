package com.redhat.agogos.core;

import com.redhat.agogos.core.v1alpha1.ComponentBuilderSpec.BuilderRef;
import com.redhat.agogos.core.v1alpha1.Pipeline.PipelineSpec.StageReference;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class AgogosEnvironment {

    public String getRunningNamespace() {
        String namespace = System.getenv("NAMESPACE");
        if (namespace == null) {
            namespace = ConfigProvider.getConfig().getValue("quarkus.kubernetes.namespace", String.class);
        }
        return namespace;
    }

    public String getRunningNamespace(BuilderRef builderRef) {
        return builderRef.getNamespace() != null ? builderRef.getNamespace() : getRunningNamespace();
    }

    public String getRunningNamespace(StageReference stageRef) {
        return stageRef.getNamespace() != null ? stageRef.getNamespace() : getRunningNamespace();
    }
}

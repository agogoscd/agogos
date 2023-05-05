package com.redhat.agogos.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "dependencies.tekton-pipeline")
public interface TektonPipelineDependency extends DependencyConfig {
}

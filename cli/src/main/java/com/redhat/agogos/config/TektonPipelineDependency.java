package com.redhat.agogos.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "dependencies.tekton-pipelines")
public interface TektonPipelineDependency extends DependencyConfig {
}

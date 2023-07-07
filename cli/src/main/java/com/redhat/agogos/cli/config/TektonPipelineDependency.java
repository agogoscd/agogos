package com.redhat.agogos.cli.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "dependencies.tekton-pipelines")
public interface TektonPipelineDependency extends DependencyConfig {
}

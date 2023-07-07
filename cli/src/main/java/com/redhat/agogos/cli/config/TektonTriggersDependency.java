package com.redhat.agogos.cli.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "dependencies.tekton-triggers")
public interface TektonTriggersDependency extends DependencyConfig {
}

package com.redhat.agogos.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "dependencies.tekton-triggers")
public interface TektonTriggersDependency extends DependencyConfig {
}

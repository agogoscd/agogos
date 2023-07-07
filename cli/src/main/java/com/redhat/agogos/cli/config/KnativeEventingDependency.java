package com.redhat.agogos.cli.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "dependencies.knative-eventing")
public interface KnativeEventingDependency extends DependencyConfig {
}

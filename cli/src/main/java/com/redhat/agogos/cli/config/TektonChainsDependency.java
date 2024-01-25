package com.redhat.agogos.cli.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "dependencies.tekton-chains")
public interface TektonChainsDependency extends DependencyConfig {
}

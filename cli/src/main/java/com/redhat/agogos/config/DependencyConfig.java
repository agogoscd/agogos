package com.redhat.agogos.config;

import java.util.List;

public abstract interface DependencyConfig {
    String namespace();

    String version();

    List<String> urls();
}

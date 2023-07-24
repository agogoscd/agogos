package com.redhat.agogos.core;

import io.fabric8.kubernetes.api.model.HasMetadata;

public class CloudEventHelper {

    // Empty and private constructor as this class is intended to 
    // have status helper methods
    private CloudEventHelper() {
    }

    public static String type(Class<? extends HasMetadata> clazz, PipelineRunState state) {
        return String.format(
                "com.redhat.agogos.event.%s.%s.v1alpha1",
                HasMetadata.getKind(clazz).toLowerCase(),
                state.toString().toLowerCase());
    }

}

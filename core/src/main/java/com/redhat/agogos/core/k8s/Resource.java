package com.redhat.agogos.core.k8s;

public enum Resource {
    BUILD,
    COMPONENT,
    PIPELINE,
    PIPELINERUN,
    RESOURCE,
    TRIGGER,
    UNKNOWN;

    public static String AGOGOS_LABEL_PREFIX = "agogos.redhat.com/";

    public String getLabel() {
        return AGOGOS_LABEL_PREFIX + this.toString().toLowerCase();
    }

    public static Resource fromType(String type) {
        if (type == null) {
            return null;
        }

        try {
            return Resource.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}

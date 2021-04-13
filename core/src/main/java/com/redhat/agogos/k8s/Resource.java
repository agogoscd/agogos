package com.redhat.agogos.k8s;

public enum Resource {
    UNKNOWN, //
    RESOURCE, //
    BUILD, //
    COMPONENT, //
    PIPELINE, //
    PIPELINERUN;

    private static String PREFIX = "agogos.redhat.com/";

    public String getLabel() {
        return PREFIX + this.toString().toLowerCase();
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

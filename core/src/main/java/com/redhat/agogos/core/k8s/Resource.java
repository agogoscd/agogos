package com.redhat.agogos.core.k8s;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@JsonSerialize(using = ToStringSerializer.class)
public enum Resource {
    BUILD("Build"),
    BUILDER("Builder"),
    COMPONENT("Component"),
    DEPENDENCY("Dependency"),
    GROUP("Group"),
    EXECUTION("Execution"),
    PIPELINE("Pipeline"),
    PIPELINERUN("PipelineRun"),
    SUBMISSION("Submission"),
    TRIGGER("Trigger"),
    UNKNOWN("Unknown");

    private final String printable;
    private final static Map<String, Resource> map = Arrays.asList(Resource.values()).stream()
            .collect(Collectors.toMap(r -> r.toString().toLowerCase(), r -> r));

    private Resource(String printable) {
        this.printable = printable;
    }

    @Override
    public String toString() {
        return printable;
    }

    public static Resource fromType(String type) {
        return type != null ? map.get(type.toLowerCase()) : null;
    }
}

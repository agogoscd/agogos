package com.redhat.cpaas.model;

import lombok.Getter;
import lombok.Setter;

public class Pipeline {
    @Getter
    @Setter
    private String name;

    public Pipeline() {

    }

    public Pipeline(io.fabric8.tekton.pipeline.v1beta1.Pipeline pipeline) {
        this.name = pipeline.getMetadata().getName();
    }
}

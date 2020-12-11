package com.redhat.cpaas.model;

import lombok.Getter;
import lombok.Setter;

public class PipelineRun {
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private Pipeline pipeline;
    @Getter
    @Setter
    private String startTime;

    public PipelineRun() {

    }

    public PipelineRun(io.fabric8.tekton.pipeline.v1beta1.PipelineRun pipelineRun) {
        this.name = pipelineRun.getMetadata().getName();
        // this.pipeline = new Pipeline(pipelineRun.getStatus().get
    }
}

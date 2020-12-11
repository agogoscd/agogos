package com.redhat.cpaas.k8s.controllers;

import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunStatus;
import io.javaoperatorsdk.operator.processing.event.AbstractEvent;
import lombok.Getter;
import lombok.ToString;

@ToString
public class PipelineRunEvent extends AbstractEvent {

    @Getter
    PipelineRunStatus status;

    public PipelineRunEvent(PipelineRun pipelineRun, PipelineRunEventSource eventSource, String ownerUid) {
        super(ownerUid, eventSource);

        status = pipelineRun.getStatus();
    }
}

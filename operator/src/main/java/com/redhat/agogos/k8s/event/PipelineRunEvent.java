package com.redhat.agogos.k8s.event;

import com.redhat.agogos.PipelineRunStatus;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunResult;
import io.javaoperatorsdk.operator.processing.event.Event;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.stream.Collectors;

@ToString
public class PipelineRunEvent extends Event {
    @Getter
    PipelineRun pipelineRun;

    @Getter
    PipelineRunStatus status;

    @Getter
    String result;

    public PipelineRunEvent(PipelineRun pipelineRun, AbstractTektonEventSource<? extends HasMetadata> eventSource,
            String ownerUid) {
        super(ResourceID.fromResource(pipelineRun));

        this.pipelineRun = pipelineRun;

        // https://tekton.dev/docs/pipelines/pipelineruns/#monitoring-execution-status
        Condition condition = pipelineRun.getStatus().getConditions().get(0);
        status = PipelineRunStatus.fromTekton(condition.getStatus(), condition.getReason());

        List<PipelineRunResult> results = pipelineRun.getStatus().getPipelineResults().stream()
                .filter(r -> r.getName().equals("data")).collect(Collectors.toUnmodifiableList());

        if (!results.isEmpty()) {
            result = results.get(0).getValue();
        }
    }

    public PipelineRunEvent(PipelineRun pipelineRun) {
        super(ResourceID.fromResource(pipelineRun));

        this.pipelineRun = pipelineRun;

        // https://tekton.dev/docs/pipelines/pipelineruns/#monitoring-execution-status
        Condition condition = pipelineRun.getStatus().getConditions().get(0);
        status = PipelineRunStatus.fromTekton(condition.getStatus(), condition.getReason());

        List<PipelineRunResult> results = pipelineRun.getStatus().getPipelineResults().stream()
                .filter(r -> r.getName().equals("data")).collect(Collectors.toUnmodifiableList());

        if (!results.isEmpty()) {
            result = results.get(0).getValue();
        }
    }
}

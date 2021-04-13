package com.redhat.agogos.k8s.event;

import com.redhat.agogos.PipelineRunStatus;
import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunResult;
import io.javaoperatorsdk.operator.processing.event.AbstractEvent;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.ToString;

@ToString
public class PipelineRunEvent extends AbstractEvent {
    @Getter
    PipelineRun pipelineRun;

    @Getter
    PipelineRunStatus status;

    @Getter
    String result;

    public PipelineRunEvent(PipelineRun pipelineRun, PipelineRunEventSource<? extends HasMetadata> eventSource,
            String ownerUid) {
        super(ownerUid, eventSource);

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

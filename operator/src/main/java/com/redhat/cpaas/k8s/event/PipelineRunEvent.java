package com.redhat.cpaas.k8s.event;

import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRunResult;
import io.javaoperatorsdk.operator.processing.event.AbstractEvent;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.ToString;

@ToString
public class PipelineRunEvent extends AbstractEvent {

    public static enum State {
        STARTED,
        RUNNING,
        CANCELLING,
        CANCELLED,
        COMPLETED,
        SUCCEEDED,
        FAILED,
        TIMEOUT;

        public static State fromTekton(String status, String reason) {
            try {
                return State.valueOf(reason.toUpperCase());
            } catch (IllegalArgumentException e) {
                State state = null;

                switch (reason) {
                    case "PipelineRunCancelled":
                        if (status.equals("Unknown")) {
                            state = CANCELLING;
                        } else {
                            state = CANCELLED;
                        }
                        break;
                    case "PipelineRunTimeout":
                        state = TIMEOUT;
                        break;
                    default:
                        // This is the case where the PipelineRun emitted a failure message as the
                        // "reason"
                        state = FAILED;
                        break;
                }

                return state;

            }
        }
    }

    @Getter
    PipelineRun pipelineRun;

    @Getter
    State state;

    @Getter
    String result;

    public PipelineRunEvent(PipelineRun pipelineRun, PipelineRunEventSource eventSource, String ownerUid) {
        super(ownerUid, eventSource);

        this.pipelineRun = pipelineRun;

        // https://tekton.dev/docs/pipelines/pipelineruns/#monitoring-execution-status
        Condition condition = pipelineRun.getStatus().getConditions().get(0);
        state = State.fromTekton(condition.getStatus(), condition.getReason());

        List<PipelineRunResult> results = pipelineRun.getStatus().getPipelineResults().stream()
                .filter(r -> r.getName().equals("data")).collect(Collectors.toUnmodifiableList());

        if (!results.isEmpty()) {
            result = results.get(0).getValue();
        }
    }
}

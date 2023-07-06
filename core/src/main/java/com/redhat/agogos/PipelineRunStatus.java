package com.redhat.agogos;

import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;

public enum PipelineRunStatus {
    STARTED,
    RUNNING,
    COMPLETED,
    SUCCEEDED,
    CANCELLING,
    CANCELLED,
    FAILED,
    TIMEOUT;

    public static PipelineRunStatus fromTekton(String conditionStatus, String conditionReason) {
        try {
            return PipelineRunStatus.valueOf(conditionReason.toUpperCase());
        } catch (IllegalArgumentException e) {
            PipelineRunStatus status = null;

            switch (conditionReason) {
                case "PipelineRunCancelled":
                    if (conditionStatus.equals("Unknown")) {
                        status = CANCELLING;
                    } else {
                        status = CANCELLED;
                    }
                    break;
                case "PipelineRunTimeout":
                    status = TIMEOUT;
                    break;
                default:
                    // This is the case where the PipelineRun emitted a failure message as the
                    // "reason"
                    status = FAILED;
                    break;
            }

            return status;

        }
    }

    public static PipelineRunStatus fromPipelineRun(PipelineRun pipelineRun) {
        Condition condition = pipelineRun.getStatus().getConditions().get(0);
        return PipelineRunStatus.fromTekton(condition.getStatus(), condition.getReason());
    }

    public PipelineRunState toEvent() {
        switch (this) {
            case STARTED:
            case RUNNING:
                return PipelineRunState.STARTED;
            case COMPLETED:
            case SUCCEEDED:
                return PipelineRunState.SUCCEEDED;
            default:
                break;
        }

        return PipelineRunState.FAILED;
    }

    public ResultableResourceStatus toStatus() {
        switch (this) {
            case STARTED:
            case RUNNING:
                return ResultableResourceStatus.RUNNING;
            case COMPLETED:
            case SUCCEEDED:
                return ResultableResourceStatus.FINISHED;
            case CANCELLING:
            case CANCELLED:
                return ResultableResourceStatus.ABORTED;
            default:
                break;
        }

        return ResultableResourceStatus.FAILED;
    }
}

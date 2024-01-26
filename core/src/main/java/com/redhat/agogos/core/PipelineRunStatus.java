package com.redhat.agogos.core;

import io.fabric8.knative.internal.pkg.apis.Condition;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;

public enum PipelineRunStatus {
    NEW, // Handle case where PipelineRun has status with no conditions.
    STARTED,
    RUNNING,
    COMPLETED,
    SUCCEEDED,
    CANCELLING,
    CANCELLED,
    RESOLVINGTASKREF,
    FAILED,
    TIMEOUT,
    UNKNOWN;

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
        if (pipelineRun.getStatus() == null) {
            return PipelineRunStatus.UNKNOWN;
        } else if (pipelineRun.getStatus().getConditions() == null || pipelineRun.getStatus().getConditions().size() == 0) {
            return PipelineRunStatus.NEW;
        }
        Condition condition = pipelineRun.getStatus().getConditions().get(0);
        return PipelineRunStatus.fromTekton(condition.getStatus(), condition.getReason());
    }

    public PipelineRunState toEvent() {
        switch (this) {
            case STARTED:
            case RESOLVINGTASKREF:
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
            case NEW:
                return ResultableResourceStatus.NEW;
            case STARTED:
            case RESOLVINGTASKREF:
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

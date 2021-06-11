package com.redhat.agogos;

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
                return ResultableResourceStatus.Running;
            case COMPLETED:
            case SUCCEEDED:
                return ResultableResourceStatus.Finished;
            case CANCELLING:
            case CANCELLED:
                return ResultableResourceStatus.Aborted;
            default:
                break;
        }

        return ResultableResourceStatus.Failed;
    }
}

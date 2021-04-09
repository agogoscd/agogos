package com.redhat.agogos.v1alpha1;

import com.redhat.agogos.PipelineRunState;
import lombok.Getter;

public enum CloudEventType {
    /**
     * Type of the CLoudEvent sent after a build is started.
     */
    BUILD_STARTED("com.redhat.agogos.event.build.started.v1alpha1"),
    /**
     * Type of the CLoudEvent sent after a successful build.
     */
    BUILD_SUCCEEDED("com.redhat.agogos.event.build.succeeded.v1alpha1"),
    /**
     * Type of the CLoudEvent sent after a failed build.
     */
    BUILD_FAILED("com.redhat.agogos.event.build.failed.v1alpha1"),
    /**
     * Type of the CLoudEvent sent after a pipeline is started.
     */
    PIPELINE_STARTED("com.redhat.agogos.event.pipeline.started.v1alpha1"),
    /**
     * Type of the CLoudEvent sent after a successful pipeline run.
     */
    PIPELINE_SUCCEEDED("com.redhat.agogos.event.pipeline.succeeded.v1alpha1"),
    /**
     * Type of the CLoudEvent sent after a pipeline failed.
     */
    PIPELINE_FAILED("com.redhat.agogos.event.pipeline.failed.v1alpha1");

    @Getter
    private String value;

    CloudEventType(String value) {
        this.value = value;
    }

    public static String forResource(AgogosResource<?, ?> resource, PipelineRunState state) {
        String type = String.format("com.redhat.agogos.event.%s.%s.v1alpha1", resource.getKind().toLowerCase(),
                state.toString().toLowerCase());

        return type;

    }

    // public static CloudEventType forResource(AgogosResource<?, ?> resource,
    // PipelineRunStatus status) {
    // if (resource == null || status == null) {
    // return null;
    // }

    // String eventType = String.format("%s_%s", resource.getKind().toUpperCase(),
    // status.toState().toString());

    // try {
    // return CloudEventType.valueOf(eventType);
    // } catch (IllegalArgumentException e) {
    // LOG.error("Could not ", t);
    // return null;
    // }
    // }
}

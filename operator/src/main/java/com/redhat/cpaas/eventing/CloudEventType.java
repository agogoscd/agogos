package com.redhat.cpaas.eventing;

import com.redhat.cpaas.cron.TriggerEventScheduler;
import lombok.Getter;

public enum CloudEventType {
    /**
     * Type of the CLoudEvent sent after a build is started.
     */
    BUILD_START("com.redhat.agogos.event.componentbuild.start.v1alpha1"),
    /**
     * Type of the CLoudEvent sent after a successful build.
     */
    BUILD_SUCCESS("com.redhat.agogos.event.componentbuild.success.v1alpha1"),
    /**
     * Type of the CLoudEvent sent after a failed build.
     */
    BUILD_FAILURE("com.redhat.agogos.event.componentbuild.failure.v1alpha1"),
    PIPELINE_START("com.redhat.agogos.event.pipelinerun.start.v1alpha1"),
    /**
     * Type of event sent when a particular trigger activation is needed.
     * 
     * @see TriggerEventScheduler
     */
    TRIGGER("com.redhat.agogos.event.trigger.v1alpha1");

    @Getter
    private String value;

    CloudEventType(String value) {
        this.value = value;
    }

}

package com.redhat.agogos.core.eventing;

import com.redhat.agogos.core.PipelineRunState;
import com.redhat.agogos.core.v1alpha1.Execution;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudEventHelper {

    private static final Logger LOG = LoggerFactory.getLogger(CloudEventHelper.class);

    // Empty and private constructor as this class is intended to 
    // have status helper methods
    private CloudEventHelper() {
    }

    public static String type(Class<? extends HasMetadata> clazz, PipelineRunState state) {
        return String.format(
                "com.redhat.agogos.event.%s.%s.v1alpha1",
                HasMetadata.getKind(clazz).toLowerCase(),
                state.toString().toLowerCase());
    }

    public static String type(Class<? extends HasMetadata> clazz, String state) {
        return String.format("com.redhat.agogos.event.%s.%s.v1alpha1", HasMetadata.getKind(clazz).toLowerCase(),
                state.toLowerCase());
    }

    public static String type(Execution execution) {
        String state;
        switch (execution.getStatus().getStatus()) {
            case ABORTED:
            case FAILED:
                state = "failed";
                break;
            case NEW:
            case RUNNING:
                state = "started";
                break;
            case FINISHED:
                state = "succeeded";
                break;
            default:
                LOG.error("Unrecognized status '{}', setting state to 'unknown'", execution.getStatus().getStatus());
                state = "unknown";
        }
        return String.format("com.redhat.agogos.event.execution.%s.v1alpha1", state);
    }
}

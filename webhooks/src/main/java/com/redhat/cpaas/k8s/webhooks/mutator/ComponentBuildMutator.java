
package com.redhat.cpaas.k8s.webhooks.mutator;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;

import com.redhat.cpaas.v1alpha1.ComponentBuildResource;
import com.redhat.cpaas.v1alpha1.ComponentResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.admission.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.AdmissionResponseBuilder;

@ApplicationScoped
public class ComponentBuildMutator extends Mutator<ComponentBuildResource> {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentBuildMutator.class);

    private static final String COMPONENT_LABEL = "cpaas.redhat.com/component";

    @Override
    protected void mutateResource(ComponentBuildResource componentBuild, AdmissionRequest request,
            AdmissionResponseBuilder responseBuilder) {

        if (!hasComponentLabel(componentBuild)) {
            setComponentLabel(componentBuild, responseBuilder);
        }

        responseBuilder.withAllowed(true);
    }

    /**
     * <p>
     * Sets the <code>cpaas.redhat.com/component</code> label pointing to the
     * {@link ComponentResource}.
     * </p>
     */
    private void setComponentLabel(ComponentBuildResource componentBuild, AdmissionResponseBuilder responseBuilder) {
        // We cannot rely on the component build name here, because at this time it will
        // not exist
        LOG.debug("Adding to new component build request '{}' label pointing to '{}' component", COMPONENT_LABEL,
                componentBuild.getSpec().getComponent());

        JsonObject labels = Json.createObjectBuilder() //
                .add(COMPONENT_LABEL, componentBuild.getSpec().getComponent()) //
                .build();

        applyPatch(responseBuilder, patchBuilder -> patchBuilder.add("/metadata/labels", labels));
    }

    /**
     * 
     * <p>
     * Checks whether the {@link ComponentBuildResource} has a
     * <code>cpaas.redhat.com/component</code> label that maps to
     * {@link ComponentResource}.
     * </p>
     * 
     * 
     * @param componentBuild A {@link ComponentBuildResource} instance
     * @return <code>true</code> is label exists, <code>false</code> otherwise
     */
    private boolean hasComponentLabel(ComponentBuildResource componentBuild) {
        Map<String, String> labels = componentBuild.getMetadata().getLabels();

        if (labels == null) {
            return false;
        }

        if (labels.get(COMPONENT_LABEL) != null) {
            return true;
        }

        return false;
    }
}

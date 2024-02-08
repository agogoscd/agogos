package com.redhat.agogos.webhooks.k8s.validator;

import com.redhat.agogos.core.AgogosEnvironment;
import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.errors.MissingResourceException;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.Stage;
import com.redhat.agogos.core.v1alpha1.StageEntry;
import com.redhat.agogos.core.v1alpha1.StageEntry.StageReference;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class PipelineValidator extends Validator<Pipeline> {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineValidator.class);

    @Inject
    AgogosEnvironment agogosEnv;

    @Override
    protected void validateResource(Pipeline pipeline, AdmissionResponseBuilder responseBuilder) {
        try {
            validateStages(pipeline);

            responseBuilder.withAllowed(true);
        } catch (ApplicationException e) {
            LOG.error("An error occurred while validating Pipeline", e);

            responseBuilder.withAllowed(false)
                    .withStatus(new StatusBuilder()
                            .withCode(e.getCode())
                            .withMessage(e.getMessage())
                            .build());
        }
    }

    private void validateStages(Pipeline pipeline) {
        // Check for the stage in the pipeline namespace, and if not there the installation namespace.
        LOG.info("Pipeline '{}' validation: validating Stages", pipeline.getFullName());

        String pnamespace = pipeline.getMetadata().getNamespace();

        for (StageEntry entry : pipeline.getSpec().getStages()) {
            StageReference stageRef = entry.getStageRef();
            String name = stageRef.getName();

            Stage stage = kubernetesFacade.get(Stage.class, pnamespace, name);
            if (stage == null) {
                String namespace = agogosEnv.getRunningNamespace();
                stage = kubernetesFacade.get(Stage.class, namespace, name);
                if (stage == null) {
                    throw new MissingResourceException("Stage '{}' is not available in namespaces '{}' or '{}'",
                            name, pnamespace, namespace);
                }
            }
        }
    }
}

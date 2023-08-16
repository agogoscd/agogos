package com.redhat.agogos.operator.k8s.controllers.pipeline;

import com.redhat.agogos.core.ResourceStatus;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.Status;
import com.redhat.agogos.operator.k8s.controllers.AbstractController;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false, dependents = {
        @Dependent(type = PipelineDependentResource.class) })
public class PipelineController extends AbstractController<Pipeline> {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineController.class);

    @Override

    public UpdateControl<Pipeline> reconcile(Pipeline pipeline, Context<Pipeline> context) {
        Status pipelineStatus = pipeline.getStatus();
        if (pipelineStatus.getStatus() != ResourceStatus.READY) {
            pipelineStatus.setStatus(ResourceStatus.READY);
            pipelineStatus.setReason("Pipeline is ready");
            pipelineStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));
            LOG.info("Set status for Pipeline '{}' to {}", pipeline.getFullName(), pipelineStatus.getStatus());
            return UpdateControl.updateStatus(pipeline);
        }

        return UpdateControl.noUpdate();
    }
}

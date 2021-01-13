package com.redhat.cpaas.k8s.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.cpaas.k8s.model.PipelineRunResource;
import com.redhat.cpaas.k8s.model.PipelineRunResource.RunStatus;
import com.redhat.cpaas.k8s.model.PipelineRunResource.Status;

import org.jboss.logging.Logger;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;

@ApplicationScoped
@Controller(crdName = "pipelineruns.cpaas.redhat.com")
public class PipelineRunController implements ResourceController<PipelineRunResource> {

    private static final Logger LOG = Logger.getLogger(PipelineRunController.class);

    @Override
    public DeleteControl deleteResource(PipelineRunResource run, Context<PipelineRunResource> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public UpdateControl<PipelineRunResource> createOrUpdateResource(PipelineRunResource run,
            Context<PipelineRunResource> context) {
        LOG.infov("PipelineRun ''{0}'' modified", run.getMetadata().getName());

        try {
            switch (Status.valueOf(run.getStatus().getStatus())) {
                case New:
                    LOG.infov("Handling new pipeline run ''{0}''", run.getMetadata().getName());

                    // Set build status to "Running"
                    setStatus(run, Status.Running, "Pipeline triggered");
                    return UpdateControl.updateStatusSubResource(run);
                default:
                    break;
            }
        } catch (Exception ex) {
            LOG.errorv(ex, "An error occurred while handling PipelineRun object ''{0}'' modification",
                    run.getMetadata().getName());

            // Set build status to "Failed"
            setStatus(run, Status.Failed, ex.getMessage());

            return UpdateControl.updateStatusSubResource(run);
        }

        return UpdateControl.noUpdate();
    }

    private boolean setStatus(PipelineRunResource run, Status status, String reason) {
        RunStatus runStatus = run.getStatus();

        if (runStatus.getStatus().equals(String.valueOf(status)) && runStatus.getReason().equals(reason)) {
            return false;
        }

        runStatus.setStatus(String.valueOf(status));
        runStatus.setReason(reason);
        runStatus.setLastUpdate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

        return true;
    }
}

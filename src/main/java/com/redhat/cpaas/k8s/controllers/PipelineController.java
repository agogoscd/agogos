package com.redhat.cpaas.k8s.controllers;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.cpaas.k8s.model.PipelineResource;

import org.jboss.logging.Logger;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;

@ApplicationScoped
@Controller(crdName = "pipelines.cpaas.redhat.com")
public class PipelineController implements ResourceController<PipelineResource> {

    private static final Logger LOG = Logger.getLogger(PipelineController.class);

    @Override
    public DeleteControl deleteResource(PipelineResource resource, Context<PipelineResource> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

    @Override
    public UpdateControl<PipelineResource> createOrUpdateResource(PipelineResource pipeline,
            Context<PipelineResource> context) {
        LOG.infov("Pipeline ''{0}'' modified", pipeline.getMetadata().getName());
        return UpdateControl.noUpdate();
    }
}

package com.redhat.cpaas.k8s.controllers;

import com.redhat.cpaas.k8s.model.ComponentGroupResource;

import org.jboss.logging.Logger;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;

@Controller
public class ComponentGroupController implements ResourceController<ComponentGroupResource> {

    private static final Logger LOG = Logger.getLogger(ComponentController.class);

    @Override
    public UpdateControl<ComponentGroupResource> createOrUpdateResource(ComponentGroupResource resource,
            Context<ComponentGroupResource> context) {
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl deleteResource(ComponentGroupResource resource, Context<ComponentGroupResource> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

}
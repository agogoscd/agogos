package com.redhat.cpaas.k8s.controllers;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.cpaas.k8s.model.ComponentGroupResource;

import org.jboss.logging.Logger;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;

@ApplicationScoped
@Controller(crdName = "groups.cpaas.redhat.com")
public class ComponentGroupController implements ResourceController<ComponentGroupResource> {

    private static final Logger LOG = Logger.getLogger(ComponentController.class);

    @Override
    public UpdateControl<ComponentGroupResource> createOrUpdateResource(ComponentGroupResource resource,
            Context<ComponentGroupResource> context) {
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl deleteResource(ComponentGroupResource resource, Context<ComponentGroupResource> context) {
        // TODO Auto-generated method stub
        return null;
    }

}

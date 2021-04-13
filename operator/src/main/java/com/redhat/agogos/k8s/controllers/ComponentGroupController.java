package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.v1alpha1.GroupResource;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;

@Controller
public class ComponentGroupController implements ResourceController<GroupResource> {

    @Override
    public UpdateControl<GroupResource> createOrUpdateResource(GroupResource resource,
            Context<GroupResource> context) {
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl deleteResource(GroupResource resource, Context<GroupResource> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

}

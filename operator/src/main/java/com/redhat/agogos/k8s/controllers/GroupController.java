package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.v1alpha1.Group;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;

@Controller
public class GroupController implements ResourceController<Group> {

    @Override
    public UpdateControl<Group> createOrUpdateResource(Group resource,
            Context<Group> context) {
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl deleteResource(Group resource, Context<Group> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

}

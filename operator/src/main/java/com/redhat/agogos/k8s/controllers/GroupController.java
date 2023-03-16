package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.v1alpha1.Group;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ControllerConfiguration
public class GroupController implements Reconciler<Group>, Cleaner<Group> {

    @Override
    public UpdateControl<Group> reconcile(Group resource,
            Context<Group> context) {
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl cleanup(Group resource, Context<Group> context) {
        return DeleteControl.defaultDelete();
    }

}

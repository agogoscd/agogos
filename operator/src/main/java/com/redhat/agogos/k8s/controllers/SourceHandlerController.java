package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.v1alpha1.SourceHandler;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;

@Controller
public class SourceHandlerController implements ResourceController<SourceHandler> {

    @Override
    public UpdateControl<SourceHandler> createOrUpdateResource(SourceHandler resource,
            Context<SourceHandler> context) {
        return UpdateControl.noUpdate();
    }

    @Override
    public DeleteControl deleteResource(SourceHandler resource, Context<SourceHandler> context) {
        return DeleteControl.DEFAULT_DELETE;
    }

}

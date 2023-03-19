package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.k8s.controllers.dependent.BuildPipelineRunDependentResource;
import com.redhat.agogos.v1alpha1.Build;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false, dependents = {
        @Dependent(type = BuildPipelineRunDependentResource.class) })
public class BuildController extends AbstractRunController<Build> {
}

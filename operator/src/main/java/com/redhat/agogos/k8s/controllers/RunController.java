package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.k8s.controllers.dependent.RunPipelineRunDependentResource;
import com.redhat.agogos.v1alpha1.Run;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false, dependents = {
        @Dependent(type = RunPipelineRunDependentResource.class) })
public class RunController<T> extends AbstractRunController<Run> {
}

package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.k8s.controllers.dependent.AgogosPipelineDependentResource;
import com.redhat.agogos.v1alpha1.Pipeline;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false, dependents = {
        @Dependent(type = AgogosPipelineDependentResource.class) })
public class PipelineController extends AbstractController<Pipeline> {
}

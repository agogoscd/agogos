package com.redhat.agogos.k8s.controllers.pipeline;

import com.redhat.agogos.k8s.controllers.AbstractController;
import com.redhat.agogos.v1alpha1.Pipeline;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false, dependents = {
        @Dependent(type = PipelineDependentResource.class) })
public class PipelineController extends AbstractController<Pipeline> {
}

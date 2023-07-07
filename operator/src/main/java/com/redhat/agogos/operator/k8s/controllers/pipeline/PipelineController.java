package com.redhat.agogos.operator.k8s.controllers.pipeline;

import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.operator.k8s.controllers.AbstractController;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false, dependents = {
        @Dependent(type = PipelineDependentResource.class) })
public class PipelineController extends AbstractController<Pipeline> {
}

package com.redhat.agogos.k8s.controllers;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.controllers.condition.ComponentReadyCondition;
import com.redhat.agogos.k8s.controllers.dependent.BuildPipelineRunDependentResource;
import com.redhat.agogos.k8s.controllers.dependent.ComponentDependentResource;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false, dependents = {
        @Dependent(type = BuildPipelineRunDependentResource.class, dependsOn = "COMPONENT"),
        @Dependent(name = "COMPONENT", type = ComponentDependentResource.class,
                   readyPostcondition = ComponentReadyCondition.class) })
public class BuildController extends AbstractRunController<Build> { 
       
    private static final Logger LOG = LoggerFactory.getLogger(BuildController.class);

    @Override
    protected Component parentResource(Build build) {
        LOG.debug("Finding parent Component for Build '{}'", build.getFullName());

        Component component = agogosClient.v1alpha1().components()
                .inNamespace(build.getMetadata().getNamespace())
                .withName(build.getSpec().getComponent()).get();

        if (component == null) {
            throw new ApplicationException("Could not find Component '{}' in namespace '{}'",
                    build.getSpec().getComponent(), build.getMetadata().getNamespace());
        }

        return component;
    }
}

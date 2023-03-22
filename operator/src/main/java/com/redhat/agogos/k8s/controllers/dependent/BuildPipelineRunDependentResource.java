package com.redhat.agogos.k8s.controllers.dependent;

import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildPipelineRunDependentResource extends AbstractPipelineRunDependentResource<Build> {

    protected static final Logger LOG = LoggerFactory.getLogger(BuildPipelineRunDependentResource.class);

    public String resourceName(Build build) {
        return build.getSpec().getComponent();
    }

    protected Component parentResource(Build build) {
        LOG.debug("Finding parent Component for Build '{}'", build.getFullName());

        Component component = agogosClient.v1alpha1().components().inNamespace(build.getMetadata().getNamespace())
                .withName(build.getSpec().getComponent()).get();

        if (component == null) {
            throw new ApplicationException("Could not find Component '{}' in namespace '{}'",
                    build.getSpec().getComponent(), build.getMetadata().getNamespace());
        }

        return component;
    }
}

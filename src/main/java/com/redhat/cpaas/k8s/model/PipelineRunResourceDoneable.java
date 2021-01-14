package com.redhat.cpaas.k8s.model;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PipelineRunResourceDoneable extends CustomResourceDoneable<PipelineRunResource> {

    public PipelineRunResourceDoneable(final PipelineRunResource resource,
            final Function<PipelineRunResource, PipelineRunResource> function) {
        super(resource, function);
    }
}

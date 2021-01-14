package com.redhat.cpaas.k8s.model;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PipelineResourceDoneable extends CustomResourceDoneable<PipelineResource> {

    public PipelineResourceDoneable(final PipelineResource resource,
            final Function<PipelineResource, PipelineResource> function) {
        super(resource, function);
    }
}

package com.redhat.cpaas.v1alpha1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@RegisterForReflection
public class PipelineRunResourceList extends CustomResourceList<PipelineRunResource> {

    private static final long serialVersionUID = 126042310656053209L;

}

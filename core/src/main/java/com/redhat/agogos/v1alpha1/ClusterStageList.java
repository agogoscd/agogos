package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@RegisterForReflection
public class ClusterStageList extends AbstractStageList<ClusterStage> {

    private static final long serialVersionUID = -4327366831588566317L;

}

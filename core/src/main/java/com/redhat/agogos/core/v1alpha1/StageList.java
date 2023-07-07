package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize
@RegisterForReflection
public class StageList extends AbstractStageList<Stage> {

    private static final long serialVersionUID = -4327366831588566317L;

}

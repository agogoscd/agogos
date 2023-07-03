package com.redhat.agogos.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.redhat.agogos.ResourceStatus;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
public class Status extends AgogosResourceStatus {

    public Status() {
        status = String.valueOf(ResourceStatus.New);
    }

}

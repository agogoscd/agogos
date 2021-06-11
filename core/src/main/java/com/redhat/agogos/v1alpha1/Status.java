package com.redhat.agogos.v1alpha1;

import com.redhat.agogos.ResourceStatus;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Status extends AgogosResourceStatus {

    private static final long serialVersionUID = -3677250631346179789L;

    public Status() {
        status = String.valueOf(ResourceStatus.New);
    }

}

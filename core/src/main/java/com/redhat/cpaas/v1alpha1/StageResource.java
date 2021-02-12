package com.redhat.cpaas.v1alpha1;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.ToString;

@ToString(callSuper = true)
@Kind("Stage")
@Group("cpaas.redhat.com")
@Version("v1alpha1")
public class StageResource extends AbstractStage implements Namespaced {
    private static final long serialVersionUID = 5548842882024636344L;

}

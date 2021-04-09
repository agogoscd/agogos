package com.redhat.agogos.v1alpha1;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.ToString;

@ToString(callSuper = true)
@Kind("Builder")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class BuilderResource extends AbstractStage implements Namespaced {
    private static final long serialVersionUID = 1184222810180288956L;

}

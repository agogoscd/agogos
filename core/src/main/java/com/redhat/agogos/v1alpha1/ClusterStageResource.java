package com.redhat.agogos.v1alpha1;

import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.ToString;

@ToString(callSuper = true)
@Kind("ClusterStage")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class ClusterStageResource extends AbstractStage {
    private static final long serialVersionUID = -3567450974238504463L;

}

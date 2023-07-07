package com.redhat.agogos.core.v1alpha1;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.ToString;

@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Kind("ClusterStage")
@Group("agogos.redhat.com")
@Version("v1alpha1")
public class ClusterStage extends AbstractStage {
    private static final long serialVersionUID = -3567450974238504463L;

}
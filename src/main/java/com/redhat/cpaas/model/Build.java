package com.redhat.cpaas.model;

import com.redhat.cpaas.k8s.model.BuildResource;

import lombok.Getter;
import lombok.Setter;

public class Build {

    @Getter
    @Setter
    private String name;

    public Build() {

    }

    public Build(BuildResource buildResource) {
        this.name = buildResource.getMetadata().getName();
    }

}

package com.redhat.cpaas.k8s;

import lombok.Getter;

public enum ResourceLabels {
    RESOURCE("agogos.redhat.com/resource"), //
    COMPONENT("agogos.redhat.com/component"), //
    PIPELINE("agogos.redhat.com/pipeline"), //
    PIPELINE_RUN("agogos.redhat.com/pipelinerun");

    @Getter
    private String value;

    ResourceLabels(String value) {
        this.value = value;
    }
}

package com.redhat.cpaas.model;

import java.util.HashMap;
import java.util.Map;

import com.redhat.cpaas.k8s.model.BuilderResource;

import lombok.Getter;
import lombok.Setter;

public class Builder {

    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String task;

    @Getter
    @Setter
    private Map<Object, Object> schema = new HashMap<>();

    // @Getter
    // @Setter
    // private BuilderStatus status = BuilderStatus.NEW;

    // @Getter
    // @Setter
    // private List<String> types = new ArrayList<>();

    public Builder() {

    }

    public Builder(BuilderResource resource) {
        this.task = resource.getSpec().getTask();
        this.name = resource.getMetadata().getName();
        //this.types.addAll(resource.getSpec().getTypes());
        this.schema = resource.getSpec().getSchema().getOpenAPIV3Schema();
        // this.status =
        // BuilderStatus.valueOf(resource.getMetadata().getAnnotations().get("status"));
    }

}

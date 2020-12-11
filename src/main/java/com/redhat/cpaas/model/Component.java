package com.redhat.cpaas.model;

import java.util.HashMap;
import java.util.Map;

import com.redhat.cpaas.k8s.model.ComponentResource;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class Component {

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String status;

    @Getter
    @Setter
    private String builder;

    // @Getter
    // @Setter
    // List<Build> builds = new ArrayList<>();

    @Getter
    @Setter
    private Map<String, String> data = new HashMap<>();

    public Component() {

    }

    public Component(ComponentResource resource) {
        this.name = resource.getMetadata().getName();
        this.status = resource.getStatus().getStatus();
        this.builder = resource.getSpec().getBuilder();

        // for (ComponentBuild build : resource.getSpec().getBuilds()) {
        // builds.add(new Build(build));
        // }

        this.data.putAll(resource.getSpec().getData());
    }
}

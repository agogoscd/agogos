package com.redhat.agogos.cli.commands.base;

import java.util.List;

import com.redhat.agogos.v1alpha1.AgogosResource;

public abstract class AbstractListCommand<T extends AgogosResource<?, ?>> implements Runnable {

    protected void list(List<T> resources) {
        resources.forEach(build -> {
            System.out.printf("%-20s %s\n", build.getMetadata().getNamespace(), build.getMetadata().getName());
        });
    }

}

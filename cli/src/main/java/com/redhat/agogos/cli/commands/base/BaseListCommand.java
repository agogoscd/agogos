package com.redhat.agogos.cli.commands.base;

import java.util.List;

import javax.inject.Inject;

import com.redhat.agogos.RunnableResourceStatus;
import com.redhat.agogos.cli.CLI.Output;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.RunnableStatus;
import com.redhat.agogos.v1alpha1.Status;

import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

public abstract class BaseListCommand<T extends AgogosResource<?, ? extends Status>> extends BaseCommand<T> {
    @Option(names = "--limit", defaultValue = "0", description = "Number of items to display, if not provided all resources will be listed")
    Long limit;

    @Inject
    MixedOperation<T, ? extends CustomResourceList<T>, Resource<T>> client;

    @Override
    public void run() {
        List<T> resources = client
                .list(new ListOptionsBuilder().withNewLimit(limit)
                        .build())
                .getItems();

        if (cli.getOutput() != null && cli.getOutput() != Output.plain) {
            printResource(resources, cli.getOutput());
            return;
        }

        printList(resources);
    }

    protected void printList(List<T> resources) {

        //System.out.println(Ansi.AUTO.string("@|bold RESOURCE\t\t\t\t STATUS |@"));

        resources.forEach(resource -> {
            if (resource.getStatus() instanceof RunnableStatus) {
                String buildColor = "green";

                if (resource.getStatus().getStatus().equals(String.valueOf(RunnableResourceStatus.Failed))) {
                    buildColor = "red";
                }

                System.out.println(Ansi.AUTO.string(String.format("@|bold %s |@\t\t @|bold,%s %s |@",
                        resource.getMetadata().getName(), buildColor, resource.getStatus().getStatus())));
            } else {
                System.out.println(Ansi.AUTO.string(String.format("@|bold %s |@\t\t @|bold %s |@",
                        resource.getMetadata().getName(), resource.getStatus().getStatus())));
            }

        });
    }
}

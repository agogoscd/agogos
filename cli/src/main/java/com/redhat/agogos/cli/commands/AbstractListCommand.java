package com.redhat.agogos.cli.commands;

import com.redhat.agogos.ResourceStatus;
import com.redhat.agogos.ResultableResourceStatus;
import com.redhat.agogos.cli.CLI.Output;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.AgogosResourceStatus;
import com.redhat.agogos.v1alpha1.ResultableStatus;
import com.redhat.agogos.v1alpha1.Status;
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import jakarta.inject.Inject;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractListCommand<T extends AgogosResource<?, ? extends AgogosResourceStatus>>
        extends AbstractSubcommand<T> {
    @Option(names = "--limit", defaultValue = "0", description = "Number of items to display, if not provided all resources will be returned.", hidden = true)
    Long limit;

    @Inject
    MixedOperation<T, ? extends DefaultKubernetesResourceList<T>, Resource<T>> client;

    @Override
    public void run() {
        List<T> resources = client
                .list(new ListOptionsBuilder().withLimit(limit)
                        .build())
                .getItems();

        if (cli.getOutput() != null && cli.getOutput() != Output.plain) {
            printResource(resources, cli.getOutput());
            return;
        }

        printList(resources);
    }

    Comparator<T> byCreationTime() {
        return (r1, r2) -> {
            if (r1.creationTime().isBefore(r2.creationTime()))
                return -1;
            else
                return 1;
        };
    }

    protected void printList(List<T> resources) {
        if (resources.isEmpty()) {
            spec.commandLine().getOut().println("No resources found");
            return;
        }

        // Find max length of the name and add some more spaces for nicer look
        final int nameColumnLength = resources.stream() //
                .mapToInt(res -> res.getMetadata().getName().length()) //
                .max() //
                .getAsInt() + 5;

        // Find max length of the status and add some more spaces for nicer look
        final int statusColumnLength = resources.stream() //
                .mapToInt(res -> res.getStatus().getStatus().length()) //
                .max() //
                .getAsInt() + 4; // It's 4 instead of 5, because we need to add a space later so that the color formatting can be escaped.

        // Header
        spec.commandLine().getOut().println(Ansi.AUTO.string(
                String.format(
                        "@|bold %-" + nameColumnLength + "." + nameColumnLength + "s%-" + statusColumnLength + "."
                                + statusColumnLength + "s CREATED|@",
                        "NAME", "STATUS")));

        // Main content
        resources.stream().sorted(byCreationTime()).forEach(resource -> {
            StringBuilder sb = new StringBuilder(
                    String.format("%-" + nameColumnLength + "." + nameColumnLength + "s",
                            resource.getMetadata().getName()));

            String color = "white";

            if (resource.getStatus() instanceof Status) {
                switch (ResourceStatus.valueOf(resource.getStatus().getStatus())) {
                    case Failed:
                        color = "red";
                        break;
                    case Ready:
                        color = "green";
                        break;
                    default:
                        break;
                }
            } else if (resource.getStatus() instanceof ResultableStatus) {
                switch (ResultableResourceStatus.valueOf(resource.getStatus().getStatus())) {
                    case Aborted:
                        color = "yellow";
                        break;
                    case Failed:
                        color = "red";
                        break;
                    case Finished:
                        color = "green";
                        break;
                    default:
                        break;
                }
            }

            sb.append(String.format("@|bold,%s %-" + statusColumnLength + "." + statusColumnLength + "s |@",
                    color,
                    resource.getStatus().getStatus()));

            sb.append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(resource.creationTime()));

            spec.commandLine().getOut().println(Ansi.AUTO.string(sb.toString()));

        });
    }
}

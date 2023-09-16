package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.CLI.Output;
import com.redhat.agogos.core.v1alpha1.AgogosResource;
import com.redhat.agogos.core.v1alpha1.AgogosResourceStatus;
import com.redhat.agogos.core.v1alpha1.ResultableStatus;
import com.redhat.agogos.core.v1alpha1.Status;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractListCommand<T extends AgogosResource<?, ? extends AgogosResourceStatus>>
        extends AbstractResourceSubcommand<T> {
    @Option(names = "--limit", defaultValue = "0", description = "Number of items to display, if not provided all resources will be returned.", hidden = true)
    protected Long limit;

    public List<T> getResources() {
        return List.of();
    }

    @Override
    public Integer call() {
        List<T> resources = getResources();
        if (cli.getOutput() != null && cli.getOutput() != Output.plain) {
            return printResource(resources, cli.getOutput());
        }

        return printList(resources);
    }

    Comparator<T> byCreationTime() {
        return (r1, r2) -> {
            if (r1.creationTime().isBefore(r2.creationTime())) {
                return -1;
            } else if (r2.creationTime().isBefore(r1.creationTime())) {
                return 1;
            }
            return 0;
        };
    }

    protected Integer printList(List<T> resources) {
        if (resources.isEmpty()) {
            spec.commandLine().getOut().println("No resources found");
            return CommandLine.ExitCode.USAGE;
        }

        // Find max length of the name and add some more spaces for nicer look
        final int nameColumnLength = resources.stream()
                .mapToInt(res -> res.getMetadata().getName().length())
                .max()
                .getAsInt() + 5;

        // Find max length of the status and add some more spaces for nicer look
        final int statusColumnLength = resources.stream()
                .mapToInt(res -> res.getStatus() instanceof Status ? ((Status) res.getStatus()).getStatus().toString().length()
                        : ((ResultableStatus) res.getStatus()).getStatus().toString().length())
                .max()
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

            String status = "";
            String color = "white";

            if (resource.getStatus() instanceof Status) {
                Status s = (Status) resource.getStatus();
                status = s.getStatus().toString();
                switch (s.getStatus()) {
                    case FAILED:
                        color = "red";
                        break;
                    case READY:
                        color = "green";
                        break;
                    default:
                        break;
                }
            } else if (resource.getStatus() instanceof ResultableStatus) {
                ResultableStatus s = (ResultableStatus) resource.getStatus();
                status = s.getStatus().toString();
                switch (s.getStatus()) {
                    case ABORTED:
                        color = "yellow";
                        break;
                    case FAILED:
                        color = "red";
                        break;
                    case FINISHED:
                        color = "green";
                        break;
                    default:
                        break;
                }
            }

            sb.append(String.format("@|bold,%s %-" + statusColumnLength + "." + statusColumnLength + "s |@",
                    color, status));

            sb.append(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(resource.creationTime()));

            spec.commandLine().getOut().println(Ansi.AUTO.string(sb.toString()));

        });

        return CommandLine.ExitCode.OK;
    }
}

package com.redhat.agogos.cli.commands.base;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.redhat.agogos.RunnableResourceStatus;
import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.CLI.Output;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.RunnableStatus;
import com.redhat.agogos.v1alpha1.Status;

import io.fabric8.kubernetes.api.model.HasMetadata;
import picocli.CommandLine.Help.Ansi;

public abstract class BaseCommand<T extends AgogosResource<?, ? extends Status>> implements Runnable {

    @Inject
    CLI cli;

    protected void print(T resource) {
        if (cli.getOutput() != null && cli.getOutput() != Output.plain) {
            printResource(resource, cli.getOutput());
            return;
        }

        describe(resource);
    }

    protected void print(List<T> resources) {
        if (cli.getOutput() != null && cli.getOutput() != Output.plain) {
            printResource(resources, cli.getOutput());
            return;
        }

        list(resources);
    }

    private void printResource(Object resource, Output output) {

        switch (output) {
            case json:
                printJson(resource);
                break;
            case yaml:
                printYaml(resource);
                break;
            default:
                break;
        }
    }

    private void list(List<T> resources) {

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

    private void describe(T resource) {
        System.out.println(Ansi.AUTO.string(String.format("%s:\t\t @|bold %s |@",
                HasMetadata.getKind(resource.getClass()), resource.getMetadata().getName())));

        if (resource.getStatus() != null) {
            if (resource.getStatus() instanceof RunnableStatus) {
                String buildColor = "green";

                if (String.valueOf(RunnableResourceStatus.Failed).equals(resource.getStatus().getStatus())) {
                    buildColor = "red";
                }

                System.out.println(Ansi.AUTO
                        .string(String.format("Status:\t\t @|bold,%s %s |@", buildColor, resource.getStatus().getStatus())));

            } else {
                System.out
                        .println(Ansi.AUTO.string(String.format("Status:\t\t @|bold %s |@", resource.getStatus().getStatus())));
            }

            System.out.println(Ansi.AUTO.string(String.format("Reason:\t\t @|bold %s |@", resource.getStatus().getReason())));
        }

    }

    private void printJson(Object resource) {
        try {
            System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(resource));
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Cannot convert resource to JSON", e);
        }
    }

    private void printYaml(Object resource) {
        try {
            System.out.println(new ObjectMapper(new YAMLFactory().disable(Feature.WRITE_DOC_START_MARKER))
                    .writerWithDefaultPrettyPrinter().writeValueAsString(resource));
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Cannot convert resource to YAML", e);
        }
    }

}

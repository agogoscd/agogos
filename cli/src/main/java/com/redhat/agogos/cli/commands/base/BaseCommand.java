package com.redhat.agogos.cli.commands.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.redhat.agogos.ResultableResourceStatus;
import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.CLI.Output;
import com.redhat.agogos.errors.ApplicationException;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.AgogosResource;
import com.redhat.agogos.v1alpha1.AgogosResourceStatus;
import com.redhat.agogos.v1alpha1.ResultableStatus;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javax.inject.Inject;
import picocli.CommandLine.Help.Ansi;

public abstract class BaseCommand<T extends AgogosResource<?, ? extends AgogosResourceStatus>> implements Runnable {

    @Inject
    CLI cli;

    @Inject
    protected AgogosClient agogosClient;

    protected void show(MixedOperation<T, ? extends CustomResourceList<T>, Resource<T>> resourceClient) {
    }

    protected void showResource(T resource) {
        if (resource == null) {
            throw new ApplicationException("No resource found");
        }

        if (cli.getOutput() != null && cli.getOutput() != Output.plain) {
            printResource(resource, cli.getOutput());
            return;
        }

        print(resource);
    }

    protected void print(T resource) {
        String nl = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();

        sb.append(Ansi.AUTO.string("💖 @|bold About|@")).append(nl).append(nl);

        sb.append(Ansi.AUTO.string(String.format("@|bold Name|@:\t\t%s", resource.getMetadata().getName()))).append(nl);

        if (resource.getStatus() != null) {
            if (resource.getStatus() instanceof ResultableStatus) {
                ResultableStatus status = (ResultableStatus) resource.getStatus();

                sb.append(nl).append(Ansi.AUTO.string("🎉 @|bold Status|@")).append(nl).append(nl);

                String buildColor = "green";

                if (String.valueOf(ResultableResourceStatus.Failed).equals(resource.getStatus().getStatus())) {
                    buildColor = "red";
                }

                ZonedDateTime startTime = status.startTime();
                ZonedDateTime completionTime = status.completionTime();

                Long duration = null;

                if (startTime != null) {
                    if (completionTime != null) {

                        duration = Duration.between(startTime, completionTime).toMinutes();
                    } else {
                        duration = Duration
                                .between(startTime, ZonedDateTime.now(ZoneId.of("UTC")))
                                .toMinutes();
                    }
                }

                sb.append(Ansi.AUTO
                        .string(String.format("@|bold Status|@:\t\t@|bold,%s %s |@", buildColor,
                                resource.getStatus().getStatus())))
                        .append(nl);
                sb.append(Ansi.AUTO.string(String.format("@|bold Reason|@:\t\t%s",
                        Optional.ofNullable(resource.getStatus().getReason()).orElse("N/A")))).append(nl);
                sb.append(Ansi.AUTO
                        .string(String.format("@|bold Created|@:\t%s",
                                formatDate(resource.creationTime()))))
                        .append(nl);
                sb.append(Ansi.AUTO.string(
                        String.format("@|bold Started|@:\t%s",
                                Optional.ofNullable(formatDate(status.startTime())).orElse("N/A"))))
                        .append(nl);
                sb.append(Ansi.AUTO.string(
                        String.format("@|bold Finished|@:\t%s",
                                Optional.ofNullable(formatDate(status.completionTime())).orElse("N/A"))))
                        .append(nl);

                if (duration != null) {
                    sb.append(Ansi.AUTO
                            .string(String.format("@|bold Duration|@:\t%s minute(s)", duration)))
                            .append(nl);
                }

                if (status.getResult() != null) {
                    sb.append(nl).append(Ansi.AUTO.string("📦 @|bold Result|@")).append(nl).append(nl);

                    sb.append(Ansi.AUTO
                            .string(String.format("%s", toJson(status.getResult()))));
                }

            } else {
                sb.append(Ansi.AUTO.string(String.format("Status:\t\t @|bold %s |@", resource.getStatus().getStatus())));
            }

        }

        System.out.println(sb.toString());

    }

    private String formatDate(ZonedDateTime time) {
        if (time == null) {
            return null;
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return dateTimeFormatter.format(time);
    }

    void printResource(Object resource, Output output) {

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

    private String toJson(Object resource) {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(resource);
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Cannot convert resource to JSON", e);
        }
    }

    private void printJson(Object resource) {
        System.out.println(toJson(resource));
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

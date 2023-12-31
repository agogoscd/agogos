package com.redhat.agogos.cli.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.cli.CLI.Output;
import com.redhat.agogos.core.ResourceStatus;
import com.redhat.agogos.core.ResultableResourceStatus;
import com.redhat.agogos.core.errors.ApplicationException;
import com.redhat.agogos.core.v1alpha1.AgogosResource;
import com.redhat.agogos.core.v1alpha1.AgogosResourceStatus;
import com.redhat.agogos.core.v1alpha1.ResultableStatus;
import com.redhat.agogos.core.v1alpha1.Status;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.quarkus.kubernetes.client.KubernetesClientObjectMapper;
import jakarta.inject.Inject;
import picocli.CommandLine;
import picocli.CommandLine.Help.Ansi;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public abstract class AbstractResourceSubcommand<T extends AgogosResource<?, ? extends AgogosResourceStatus>>
        extends AbstractCallableSubcommand {

    // This mapper is *only* included here to allow for pretty printing of JSON. All other
    // mappers in Agogos should use KubernetesSerialization.
    @KubernetesClientObjectMapper
    @Inject
    ObjectMapper mapper;

    @Inject
    protected KubernetesSerialization objectMapper;

    protected Integer showResource(T resource) {
        if (resource == null) {
            helper.printStderr("⛔ No resource found.");
            return CommandLine.ExitCode.USAGE;
        }

        if (cli.getOutput() != null && cli.getOutput() != Output.plain) {
            return printResource(resource, cli.getOutput());
        }

        return print(resource);
    }

    protected Integer print(T resource) {
        String nl = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();

        sb.append(Ansi.AUTO.string("💖 @|bold About|@")).append(nl).append(nl);

        sb.append(Ansi.AUTO.string(String.format("@|bold Name|@:\t\t%s", resource.getMetadata().getName()))).append(nl);

        if (resource.getStatus() != null) {
            if (resource.getStatus() instanceof ResultableStatus) {
                ResultableStatus status = (ResultableStatus) resource.getStatus();

                sb.append(nl).append(Ansi.AUTO.string("🎉 @|bold Status|@")).append(nl).append(nl);

                String color = "green";

                if (status.getStatus() == ResultableResourceStatus.FAILED) {
                    color = "red";
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
                        .string(String.format("@|bold Status|@:\t\t@|bold,%s %s |@", color,
                                status.getStatus())))
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
                Status status = (Status) resource.getStatus();
                if (status != null) {
                    String color = "";
                    if (status.getStatus() == ResourceStatus.READY) {
                        color = "green";
                    }
                    sb.append(
                            Ansi.AUTO.string(String.format("@|bold Status|@:\t\t@|bold,%s %s |@", color, status.getStatus())))
                            .append(nl);
                }
                sb.append(Ansi.AUTO
                        .string(String.format("@|bold Created|@:\t%s",
                                formatDate(resource.creationTime()))))
                        .append(nl);
            }

        }

        helper.printStdout(sb.toString());
        return CommandLine.ExitCode.OK;
    }

    protected String formatDate(ZonedDateTime time) {
        if (time == null) {
            return null;
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return dateTimeFormatter.format(time);
    }

    protected Integer printResource(Object resource, Output output) {

        switch (output) {
            case json:
                helper.printStdout(toJson(resource));
                break;
            case yaml:
                helper.printStdout(objectMapper.asYaml(resource));
                break;
            default:
                break;
        }
        return CommandLine.ExitCode.OK;
    }

    protected String toJson(Object resource) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resource);
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Cannot convert resource to JSON", e);
        }
    }
}

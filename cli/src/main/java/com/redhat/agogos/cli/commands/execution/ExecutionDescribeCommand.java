package com.redhat.agogos.cli.commands.execution;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.core.ResultableResourceStatus;
import com.redhat.agogos.core.v1alpha1.Execution;
import com.redhat.agogos.core.v1alpha1.Execution.ExecutionInfo;
import com.redhat.agogos.core.v1alpha1.ResultableStatus;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Command(mixinStandardHelpOptions = true, name = "describe", aliases = { "d", "desc" }, description = "describe execution")
public class ExecutionDescribeCommand extends AbstractResourceSubcommand<Execution> {

    @Option(names = { "-l", "--last" }, description = "Show description for last execution")
    boolean last;

    @Parameters(arity = "0..1", description = "Name of the execution")
    String name;

    @Override
    public Integer call() {
        Execution execution = null;

        if (last) {
            List<Execution> resources = kubernetesFacade.list(Execution.class, kubernetesFacade.getNamespace());
            execution = resources.stream().sorted(byCreationTime()).findFirst().get();
        } else if (name != null) {
            System.out.println("+++++++++++++++++++++++++++++++++++++ HERE");
            execution = kubernetesFacade.get(Execution.class, kubernetesFacade.getNamespace(), name);
        }

        return showResource(execution);
    }

    Comparator<Execution> byCreationTime() {
        return (r1, r2) -> {
            return r2.creationTime().compareTo(r1.creationTime());
        };
    }

    @Override
    protected Integer print(Execution execution) {
        String nl = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();

        sb.append(Ansi.AUTO.string("💖 @|bold About|@")).append(nl).append(nl);

        sb.append(Ansi.AUTO.string(String.format("@|bold Name|@:\t\t%s", execution.getMetadata().getName())))
                .append(nl)
                .append(nl);

        addDependentLines(sb, execution.getSpec().getComponents(), "Components");
        addDependentLines(sb, execution.getSpec().getGroups(), "Groups");
        addDependentLines(sb, execution.getSpec().getPipelines(), "Pipelines");

        if (execution.getStatus() != null) {
            ResultableStatus status = (ResultableStatus) execution.getStatus();

            sb.append(nl).append(Ansi.AUTO.string("🎉 @|bold Status|@")).append(nl).append(nl);

            String color = "green";

            if (status.getStatus() == ResultableResourceStatus.FAILED) {
                color = "red";
            }

            ZonedDateTime startTime = status.startTime();
            ZonedDateTime completionTime = status.completionTime();

            Long duration = durationInMinutes(startTime, completionTime);

            sb.append(Ansi.AUTO
                    .string(String.format("@|bold Status|@:\t\t@|bold,%s %s |@", color,
                            status.getStatus())))
                    .append(nl);
            sb.append(Ansi.AUTO.string(String.format("@|bold Reason|@:\t\t%s",
                    Optional.ofNullable(execution.getStatus().getReason()).orElse("N/A")))).append(nl);
            sb.append(Ansi.AUTO
                    .string(String.format("@|bold Created|@:\t%s",
                            formatDate(execution.creationTime()))))
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
                        .string(String.format("%s", toJson(status.getResult()))))
                        .append(nl);
            }
        }

        spec.commandLine().getOut().println(sb.toString());

        return CommandLine.ExitCode.OK;
    }

    private void addDependentLines(StringBuilder sb, Map<String, ExecutionInfo> dependents, String kind) {
        if (dependents.size() > 0) {
            String nl = System.getProperty("line.separator");
            sb.append(Ansi.AUTO.string(
                    String.format("@|bold %s|@:@|bold \t%-30.30s  %-8s  %-19s  %-19s  %-15s|@", kind, "Name", "Status",
                            "Started",
                            "Completed", "Duration")))
                    .append(nl);
            dependents.entrySet().stream().forEach(e -> {
                ExecutionInfo info = e.getValue();
                ResultableStatus status = info.getStatus();
                ZonedDateTime startTime = status.startTime();
                ZonedDateTime completionTime = status.completionTime();
                Long duration = durationInMinutes(startTime, completionTime);
                sb.append(Ansi.AUTO.string(
                        String.format("\t\t%-30.30s  %-8s  %-19s  %-19s  %-15s", e.getKey(),
                                status.getStatus(),
                                Optional.ofNullable(formatDate(startTime)).orElse("N/A"),
                                Optional.ofNullable(formatDate(completionTime)).orElse("N/A"),
                                duration != null ? duration.toString() + " minute(s)" : "")))
                        .append(nl);
            });
        }
    }

    private Long durationInMinutes(ZonedDateTime startTime, ZonedDateTime completionTime) {
        if (startTime != null) {
            if (completionTime != null) {
                return Duration.between(startTime, completionTime).toMinutes();
            } else {
                return Duration
                        .between(startTime, ZonedDateTime.now(ZoneId.of("UTC")))
                        .toMinutes();
            }
        }
        return null;
    }
}

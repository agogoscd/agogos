package com.redhat.agogos.cli.commands.run;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.core.v1alpha1.Run;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Comparator;
import java.util.List;

@Command(mixinStandardHelpOptions = true, name = "describe", description = "describe run")
public class RunDescribeCommand extends AbstractResourceSubcommand<Run> {

    @Option(names = { "-l", "--last" }, description = "Show description for last run")
    boolean last;

    @Parameters(arity = "0..1", description = "Name of the run")
    String name;

    @Override
    public Integer call() {
        Run run = null;

        if (last) {
            List<Run> resources = kubernetesFacade.list(Run.class, kubernetesFacade.getNamespace());
            run = resources.stream().sorted(byCreationTime()).findFirst().get();
        } else if (name != null) {
            run = kubernetesFacade.get(Run.class, kubernetesFacade.getNamespace(), name);
        }

        return showResource(run);
    }

    Comparator<Run> byCreationTime() {
        return (r1, r2) -> {
            return r2.creationTime().compareTo(r1.creationTime());
        };
    }
}

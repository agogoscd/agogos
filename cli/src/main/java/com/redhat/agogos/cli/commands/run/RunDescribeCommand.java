package com.redhat.agogos.cli.commands.run;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.core.v1alpha1.Run;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "describe", description = "describe run")
public class RunDescribeCommand extends AbstractResourceSubcommand<Run> {
    @Parameters(index = "0", description = "Name of the run")
    String name;

    @Override
    public void run() {
        Run run = kubernetesFacade.get(Run.class, kubernetesFacade.getNamespace(), name);
        showResource(run);
    }
}

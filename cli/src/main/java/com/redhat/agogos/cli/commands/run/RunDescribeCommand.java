package com.redhat.agogos.cli.commands.run;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.core.k8s.client.AgogosClient;
import com.redhat.agogos.core.v1alpha1.Run;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "describe", description = "describe run")
public class RunDescribeCommand extends AbstractResourceSubcommand<Run> {
    @Parameters(index = "0", description = "Name of the run")
    String name;

    @Inject
    AgogosClient agogosClient;

    @Override
    public void run() {
        Run run = agogosClient.v1alpha1().runs().inNamespace(agogosClient.namespace()).withName(name).get();
        showResource(run);
    }
}
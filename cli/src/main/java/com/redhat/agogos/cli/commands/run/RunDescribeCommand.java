package com.redhat.agogos.cli.commands.run;

import javax.inject.Inject;

import com.redhat.agogos.cli.commands.AbstractSubcommand;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Run;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "describe", description = "describe run")
public class RunDescribeCommand extends AbstractSubcommand<Run> {
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
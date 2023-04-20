package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.commands.RunCommand.RunDescribeCommand;
import com.redhat.agogos.cli.commands.RunCommand.RunListCommand;
import com.redhat.agogos.cli.commands.base.AbstractCommand;
import com.redhat.agogos.cli.commands.base.AbstractSubcommand;
import com.redhat.agogos.cli.commands.base.BaseListCommand;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Run;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import javax.inject.Inject;

@Command(mixinStandardHelpOptions = true, name = "runs", aliases = {
        "run", "r" }, description = "Interact with runs", subcommands = { // 
                RunDescribeCommand.class,
                RunListCommand.class
        })
public class RunCommand extends AbstractCommand {

    @Command(mixinStandardHelpOptions = true, name = "describe", description = "describe run")
    public static class RunDescribeCommand extends AbstractSubcommand<Run> {
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

    @Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list runs")
    static class RunListCommand extends BaseListCommand<Run> {
    }
}

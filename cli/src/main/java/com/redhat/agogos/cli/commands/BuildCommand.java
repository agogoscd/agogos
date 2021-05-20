package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.commands.BuildCommand.BuildDescribeCommand;
import com.redhat.agogos.cli.commands.BuildCommand.BuildListCommand;
import com.redhat.agogos.cli.commands.base.BaseCommand;
import com.redhat.agogos.cli.commands.base.BaseListCommand;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Build;
import javax.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "build", aliases = {
        "builds", "b" }, description = "Interact with builds", subcommands = { // 
                BuildDescribeCommand.class,
                BuildListCommand.class
        })
public class BuildCommand implements Runnable {

    @Inject
    CLI cli;

    @Override
    public void run() {
        cli.usage(this.getClass());
    }

    @Command(mixinStandardHelpOptions = true, name = "describe", description = "describe build")
    public static class BuildDescribeCommand extends BaseCommand<Build> {
        @Parameters(index = "0", description = "Name of the build")
        String name;

        @Inject
        AgogosClient agogosClient;

        @Override
        public void run() {
            Build build = agogosClient.v1alpha1().builds().inNamespace(agogosClient.namespace()).withName(name).get();
            showResource(build);
        }
    }

    @Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list builds")
    static class BuildListCommand extends BaseListCommand<Build> {

    }
}

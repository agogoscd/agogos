package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.commands.BuildCommand.BuildDescribeCommand;
import com.redhat.agogos.cli.commands.ComponentCommand.ComponentBuildCommand;
import com.redhat.agogos.cli.commands.ComponentCommand.ComponentDescribeCommand;
import com.redhat.agogos.cli.commands.ComponentCommand.ComponentListCommand;
import com.redhat.agogos.cli.commands.base.BaseCommand;
import com.redhat.agogos.cli.commands.base.BaseListCommand;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import javax.inject.Inject;

@Command(mixinStandardHelpOptions = true, name = "components", aliases = {
        "component", "c" }, description = "Interact with components", subcommands = { // 
                ComponentBuildCommand.class,
                ComponentListCommand.class,
                ComponentDescribeCommand.class
        })
public class ComponentCommand implements Runnable {
    @Inject
    CLI cli;

    @Override
    public void run() {
        cli.usage(this.getClass());
    }

    @Command(mixinStandardHelpOptions = true, name = "describe", aliases = { "desc", "d" }, description = "describe component")
    public static class ComponentDescribeCommand extends BaseCommand<Component> {
        @Parameters(index = "0", description = "Name of the component")
        String name;

        @Inject
        AgogosClient agogosClient;

        @Override
        public void run() {
            Component component = agogosClient.v1alpha1().components().withName(name).get();
            showResource(component);
        }
    }

    @Command(mixinStandardHelpOptions = true, name = "build", aliases = { "b" }, description = "build a component")
    static class ComponentBuildCommand extends BaseCommand<Component> {
        @Parameters(index = "0", description = "Name of the component to build.")
        String name;

        @Inject
        AgogosClient agogosClient;

        @Inject
        CLI cli;

        @Override
        public void run() {
            Build build = new Build();

            build.getMetadata().setGenerateName(name + "-");
            build.getSpec().setComponent(name);

            build = agogosClient.v1alpha1().builds().inNamespace(agogosClient.namespace()).resource(build).create();

            cli.run(BuildDescribeCommand.class, build.getMetadata().getName());
        }
    }

    @Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list components")
    static class ComponentListCommand extends BaseListCommand<Component> {
    }
}

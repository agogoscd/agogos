package com.redhat.agogos.cli.commands;

import java.util.List;

import javax.inject.Inject;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.commands.BuildCommand.BuildDescribeCommand;
import com.redhat.agogos.cli.commands.ComponentCommand.ComponentBuildCommand;
import com.redhat.agogos.cli.commands.ComponentCommand.ComponentListCommand;
import com.redhat.agogos.cli.commands.base.BaseCommand;
import com.redhat.agogos.cli.commands.base.ListMixin;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;

import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "components", aliases = {
        "component", "c" }, description = "Interact with components", subcommands = { // 
                ComponentBuildCommand.class,
                ComponentListCommand.class
        })
public class ComponentCommand implements Runnable {
    @Inject
    CLI cli;

    @Override
    public void run() {
        cli.run(ComponentListCommand.class);
    }

    @Command(mixinStandardHelpOptions = true, name = "describe", description = "describe component")
    public static class ComponentDescribeCommand extends BaseCommand<Component> {
        @Parameters(index = "0", description = "Name of the component")
        String name;

        @Inject
        AgogosClient agogosClient;

        @Override
        public void run() {
            Component component = agogosClient.v1alpha1().components().withName(name).get();
            print(component);
        }
    }

    @Command(mixinStandardHelpOptions = true, name = "build", description = "build a component")
    static class ComponentBuildCommand extends BaseCommand<Component> {
        @Parameters(index = "0", description = "Name of the component to build")
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

            build = agogosClient.v1alpha1().builds().inNamespace(agogosClient.namespace()).create(build);

            cli.run(BuildDescribeCommand.class, build.getMetadata().getName());
        }
    }

    @Command(mixinStandardHelpOptions = true, name = "list", description = "list components")
    static class ComponentListCommand extends BaseCommand<Component> {

        @Mixin
        ListMixin list;

        @Inject
        AgogosClient agogosClient;

        @Override
        public void run() {
            List<Component> components = agogosClient.v1alpha1().components()
                    .list(new ListOptionsBuilder().withNewLimit(list.getLimit())
                            .build())
                    .getItems();

            print(components);
        }
    }
}

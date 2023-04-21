package com.redhat.agogos.cli.commands.component;

import com.redhat.agogos.cli.commands.AbstractSubcommand;
import com.redhat.agogos.cli.commands.build.BuildDescribeCommand;
import com.redhat.agogos.v1alpha1.Build;
import com.redhat.agogos.v1alpha1.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "build", aliases = { "b" }, description = "build a component")
public class ComponentBuildCommand extends AbstractSubcommand<Component> {
    @Parameters(index = "0", description = "Name of the component to build.")
    String name;

    @Override
    public void run() {
        Build build = new Build();

        build.getMetadata().setGenerateName(name + "-");
        build.getSpec().setComponent(name);

        build = agogosClient.v1alpha1().builds().inNamespace(agogosClient.namespace()).resource(build).create();

        cli.run(BuildDescribeCommand.class, build.getMetadata().getName());
    }
}
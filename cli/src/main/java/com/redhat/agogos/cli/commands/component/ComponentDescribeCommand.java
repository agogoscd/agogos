package com.redhat.agogos.cli.commands.component;

import com.redhat.agogos.cli.commands.AbstractSubcommand;
import com.redhat.agogos.v1alpha1.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "describe", aliases = { "desc", "d" }, description = "describe component")
public class ComponentDescribeCommand extends AbstractSubcommand<Component> {
    @Parameters(index = "0", description = "Name of the component")
    String name;

    @Override
    public void run() {
        Component component = agogosClient.v1alpha1().components().withName(name).get();
        showResource(component);
    }
}
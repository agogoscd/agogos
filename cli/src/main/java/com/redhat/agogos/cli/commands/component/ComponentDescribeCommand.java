package com.redhat.agogos.cli.commands.component;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.core.v1alpha1.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "describe", aliases = { "desc", "d" }, description = "describe component")
public class ComponentDescribeCommand extends AbstractResourceSubcommand<Component> {
    @Parameters(index = "0", description = "Name of the component")
    String name;

    @Override
    public void run() {
        Component component = kubernetesFacade.get(Component.class, kubernetesFacade.getNamespace(), name);
        showResource(component);
    }
}
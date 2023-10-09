package com.redhat.agogos.cli.commands.trigger;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.core.v1alpha1.triggers.Trigger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "describe", aliases = { "desc", "d" }, description = "describe trigger")
public class TriggerDescribeCommand extends AbstractResourceSubcommand<Trigger> {
    @Parameters(index = "0", description = "Name of the Trigger")
    String name;

    @Override
    public Integer call() {
        Trigger trigger = kubernetesFacade.get(Trigger.class, kubernetesFacade.getNamespace(), name);
        return showResource(trigger);
    }
}

package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.commands.TriggerCommand.TriggerListCommand;
import com.redhat.agogos.cli.commands.base.AbstractCommand;
import com.redhat.agogos.cli.commands.base.BaseListCommand;
import com.redhat.agogos.v1alpha1.triggers.Trigger;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "triggers", aliases = {
        "trigger", "t" }, description = "Interact with triggers", subcommands = { // 
                TriggerListCommand.class
        })
public class TriggerCommand extends AbstractCommand {

    @Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list triggers")
    static class TriggerListCommand extends BaseListCommand<Trigger> {
    }
}

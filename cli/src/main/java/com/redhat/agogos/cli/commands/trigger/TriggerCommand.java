package com.redhat.agogos.cli.commands.trigger;

import com.redhat.agogos.cli.commands.AbstractSubcommand;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "triggers", aliases = {
        "trigger", "t" }, description = "Interact with triggers", subcommands = { // 
                TriggerDescribeCommand.class,
                TriggerListCommand.class
        })
public class TriggerCommand extends AbstractSubcommand {
}

package com.redhat.agogos.cli.commands.trigger;

import com.redhat.agogos.cli.commands.AbstractCommand;

import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "triggers", aliases = {
        "trigger", "t" }, description = "Interact with triggers", subcommands = { // 
                TriggerListCommand.class
        })
public class TriggerCommand extends AbstractCommand {
}

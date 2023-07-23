package com.redhat.agogos.cli.commands.group;

import com.redhat.agogos.cli.commands.AbstractSubcommand;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "groups", aliases = {
        "group", "g" }, description = "Interact with groups", subcommands = {
                GroupExecuteCommand.class,
                GroupListCommand.class,
                GroupDescribeCommand.class
        })
public class GroupCommand extends AbstractSubcommand {
}

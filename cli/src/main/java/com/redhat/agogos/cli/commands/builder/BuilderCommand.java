package com.redhat.agogos.cli.commands.builder;

import com.redhat.agogos.cli.commands.AbstractSubcommand;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "builder", aliases = {
        "builders", "bldr" }, description = "Interact with builders", subcommands = {
                BuilderDescribeCommand.class,
                BuilderListCommand.class
        })
public class BuilderCommand extends AbstractSubcommand {
}

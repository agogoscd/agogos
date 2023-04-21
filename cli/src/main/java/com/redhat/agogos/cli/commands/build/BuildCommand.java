package com.redhat.agogos.cli.commands.build;

import com.redhat.agogos.cli.commands.AbstractCommand;

import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "build", aliases = {
        "builds", "b" }, description = "Interact with builds", subcommands = { // 
                BuildDescribeCommand.class,
                BuildListCommand.class
        })
public class BuildCommand extends AbstractCommand {
}

package com.redhat.agogos.cli.commands.run;

import com.redhat.agogos.cli.commands.AbstractCommand;

import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "runs", aliases = {
        "run", "r" }, description = "Interact with runs", subcommands = { // 
                RunDescribeCommand.class,
                RunListCommand.class
        })
public class RunCommand extends AbstractCommand {
}

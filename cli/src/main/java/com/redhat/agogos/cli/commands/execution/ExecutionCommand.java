package com.redhat.agogos.cli.commands.execution;

import com.redhat.agogos.cli.commands.AbstractSubcommand;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "executions", aliases = {
        "execs", "exec", "e" }, description = "Interact with executions", subcommands = {
                ExecutionDescribeCommand.class,
                ExecutionListCommand.class
        })
public class ExecutionCommand extends AbstractSubcommand {
}

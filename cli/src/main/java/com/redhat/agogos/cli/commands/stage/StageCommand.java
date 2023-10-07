package com.redhat.agogos.cli.commands.stage;

import com.redhat.agogos.cli.commands.AbstractSubcommand;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "stage", aliases = {
        "stages", "stg" }, description = "Interact with stages", subcommands = { // 
                StageDescribeCommand.class,
                StageListCommand.class
        })
public class StageCommand extends AbstractSubcommand {
}

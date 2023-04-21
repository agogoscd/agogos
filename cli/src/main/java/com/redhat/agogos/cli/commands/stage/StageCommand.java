package com.redhat.agogos.cli.commands.stage;

import com.redhat.agogos.cli.commands.AbstractCommand;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "stage", aliases = {
        "stages", "stg" }, description = "Interact with stages", subcommands = { // 
                StageListCommand.class
        })
public class StageCommand extends AbstractCommand {
}

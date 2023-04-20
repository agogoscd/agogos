package com.redhat.agogos.cli.commands.pipeline;

import com.redhat.agogos.cli.commands.AbstractCommand;

import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "pipelines", aliases = {
        "pipeline", "p" }, description = "Interact with pipelines", subcommands = { // 
                PipelineListCommand.class,
                PipelineRunCommand.class
        })
public class PipelineCommand extends AbstractCommand {
}

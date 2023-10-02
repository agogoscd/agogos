package com.redhat.agogos.cli.commands.pipeline;

import com.redhat.agogos.cli.commands.AbstractSubcommand;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, name = "pipelines", aliases = {
        "pipeline", "p" }, description = "Interact with pipelines", subcommands = {
                PipelineDescribeCommand.class,
                PipelineListCommand.class,
                PipelineRunCommand.class
        })
public class PipelineCommand extends AbstractSubcommand {
}

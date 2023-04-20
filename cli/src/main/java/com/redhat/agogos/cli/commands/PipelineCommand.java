package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.commands.PipelineCommand.PipelineListCommand;
import com.redhat.agogos.cli.commands.PipelineCommand.PipelineRunCommand;
import com.redhat.agogos.cli.commands.RunCommand.RunDescribeCommand;
import com.redhat.agogos.cli.commands.base.AbstractCommand;
import com.redhat.agogos.cli.commands.base.AbstractSubcommand;
import com.redhat.agogos.cli.commands.base.BaseListCommand;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.Run;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "pipelines", aliases = {
        "pipeline", "p" }, description = "Interact with pipelines", subcommands = { // 
                PipelineListCommand.class,
                PipelineRunCommand.class
        })
public class PipelineCommand extends AbstractCommand {

    @Command(mixinStandardHelpOptions = true, name = "run", description = "run a pipeline")
    static class PipelineRunCommand extends AbstractSubcommand<Pipeline> {
        @Parameters(index = "0", description = "Name of the pipeline to run")
        String name;

        @Override
        public void run() {
            Run run = new Run();

            run.getMetadata().setGenerateName(name + "-");
            run.getSpec().setPipeline(name);

            run = agogosClient.v1alpha1().runs().inNamespace(agogosClient.namespace()).resource(run).create();

            cli.run(RunDescribeCommand.class, run.getMetadata().getName());
        }
    }

    @Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list pipelines")
    static class PipelineListCommand extends BaseListCommand<Pipeline> {
    }
}

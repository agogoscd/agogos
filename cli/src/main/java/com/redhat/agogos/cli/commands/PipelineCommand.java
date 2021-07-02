package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.commands.PipelineCommand.PipelineListCommand;
import com.redhat.agogos.cli.commands.PipelineCommand.PipelineRunCommand;
import com.redhat.agogos.cli.commands.RunCommand.RunDescribeCommand;
import com.redhat.agogos.cli.commands.base.BaseCommand;
import com.redhat.agogos.cli.commands.base.BaseListCommand;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.Run;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import javax.inject.Inject;

@Command(mixinStandardHelpOptions = true, name = "pipelines", aliases = {
        "pipeline", "p" }, description = "Interact with pipelines", subcommands = { // 
                PipelineListCommand.class,
                PipelineRunCommand.class
        })
public class PipelineCommand implements Runnable {
    @Inject
    CLI cli;

    @Override
    public void run() {
        cli.usage(this.getClass());
    }

    @Command(mixinStandardHelpOptions = true, name = "run", description = "run a pipeline")
    static class PipelineRunCommand extends BaseCommand<Pipeline> {
        @Parameters(index = "0", description = "Name of the pipeline to run")
        String name;

        @Inject
        AgogosClient agogosClient;

        @Inject
        CLI cli;

        @Override
        public void run() {
            Run run = new Run();

            run.getMetadata().setGenerateName(name + "-");
            run.getSpec().setPipeline(name);

            run = agogosClient.v1alpha1().runs().inNamespace(agogosClient.namespace()).create(run);

            cli.run(RunDescribeCommand.class, run.getMetadata().getName());
        }
    }

    @Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list pipelines")
    static class PipelineListCommand extends BaseListCommand<Pipeline> {
    }
}

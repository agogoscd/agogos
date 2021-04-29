package com.redhat.agogos.cli.commands;

import java.util.List;

import javax.inject.Inject;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.commands.ComponentCommand.ComponentListCommand;
import com.redhat.agogos.cli.commands.PipelineCommand.PipelineListCommand;
import com.redhat.agogos.cli.commands.PipelineCommand.PipelineRunCommand;
import com.redhat.agogos.cli.commands.base.BaseCommand;
import com.redhat.agogos.cli.commands.base.ListMixin;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.Run;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Parameters;

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
        cli.run(ComponentListCommand.class);
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

            cli.run(PipelineListCommand.class);
        }
    }

    @Command(mixinStandardHelpOptions = true, name = "list", description = "list pipelines")
    static class PipelineListCommand extends BaseCommand<Pipeline> {

        @Mixin
        ListMixin list;

        @Inject
        AgogosClient agogosClient;

        @Override
        public void run() {
            List<Pipeline> pipelines = agogosClient.v1alpha1().pipelines().list()
                    .getItems();

            print(pipelines);
        }
    }
}

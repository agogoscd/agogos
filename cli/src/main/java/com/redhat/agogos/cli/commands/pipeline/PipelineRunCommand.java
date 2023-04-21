package com.redhat.agogos.cli.commands.pipeline;

import com.redhat.agogos.cli.commands.AbstractSubcommand;
import com.redhat.agogos.cli.commands.run.RunDescribeCommand;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.Run;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "run", description = "run a pipeline")
public class PipelineRunCommand extends AbstractSubcommand<Pipeline> {
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
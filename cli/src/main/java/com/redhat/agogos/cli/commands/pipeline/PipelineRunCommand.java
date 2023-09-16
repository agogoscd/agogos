package com.redhat.agogos.cli.commands.pipeline;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.cli.commands.run.RunDescribeCommand;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.Run;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "run", description = "run a pipeline")
public class PipelineRunCommand extends AbstractResourceSubcommand<Pipeline> {
    @Parameters(index = "0", description = "Name of the pipeline to run")
    String name;

    @Override
    public Integer call() {
        Run run = new Run();

        run.getMetadata().setGenerateName(name + "-");
        run.getMetadata().setNamespace(kubernetesFacade.getNamespace());
        run.getSpec().setPipeline(name);

        run = kubernetesFacade.create(run);
        return cli.run(RunDescribeCommand.class, run.getMetadata().getName());
    }
}

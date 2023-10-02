package com.redhat.agogos.cli.commands.pipeline;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "describe", aliases = { "desc", "d" }, description = "describe pipeline")
public class PipelineDescribeCommand extends AbstractResourceSubcommand<Pipeline> {
    @Parameters(index = "0", description = "Name of the pipeline")
    String name;

    @Override
    public Integer call() {
        Pipeline pipeline = kubernetesFacade.get(Pipeline.class, kubernetesFacade.getNamespace(), name);
        return showResource(pipeline);
    }
}

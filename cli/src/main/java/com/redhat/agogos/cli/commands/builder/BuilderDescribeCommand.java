package com.redhat.agogos.cli.commands.builder;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.core.v1alpha1.Builder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "describe", aliases = { "d", "desc" }, description = "describe builder")
public class BuilderDescribeCommand extends AbstractResourceSubcommand<Builder> {

    @Parameters(description = "Name of the builder")
    String name;

    @Override
    public Integer call() {
        Builder builder = null;

        builder = getBuilder(kubernetesFacade.getNamespace());
        if (builder == null) {
            builder = getBuilder(agogosEnvironment.getRunningNamespace());
        }

        return showResource(builder);
    }

    private Builder getBuilder(String namespace) {
        return kubernetesFacade.get(Builder.class, namespace, name);
    }
}

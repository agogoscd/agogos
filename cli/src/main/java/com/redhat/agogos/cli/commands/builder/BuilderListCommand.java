package com.redhat.agogos.cli.commands.builder;

import com.redhat.agogos.cli.commands.AbstractListCommand;
import com.redhat.agogos.core.v1alpha1.Builder;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import picocli.CommandLine.Command;

import java.util.List;

@Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list builders")
public class BuilderListCommand extends AbstractListCommand<Builder> {

    @Override
    public List<Builder> getResources() {
        List<Builder> resources = kubernetesFacade.list(
                Builder.class,
                kubernetesFacade.getNamespace(),
                new ListOptionsBuilder().withLimit(limit).build());
        return resources;
    }
}

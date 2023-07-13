package com.redhat.agogos.cli.commands.build;

import com.redhat.agogos.cli.commands.AbstractListCommand;
import com.redhat.agogos.core.v1alpha1.Build;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import picocli.CommandLine.Command;

import java.util.List;

@Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list builds")
public class BuildListCommand extends AbstractListCommand<Build> {

    @Override
    public List<Build> getResources() {
        List<Build> resources = kubernetesFacade.list(
                Build.class,
                kubernetesFacade.getNamespace(),
                new ListOptionsBuilder().withLimit(limit).build());
        return resources;
    }

}
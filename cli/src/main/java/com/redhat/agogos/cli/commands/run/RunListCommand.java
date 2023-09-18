package com.redhat.agogos.cli.commands.run;

import com.redhat.agogos.cli.commands.AbstractListCommand;
import com.redhat.agogos.core.v1alpha1.Run;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import picocli.CommandLine.Command;

import java.util.List;

@Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list runs")
public class RunListCommand extends AbstractListCommand<Run> {

    @Override
    public List<Run> getResources() {
        List<Run> resources = kubernetesFacade.list(
                Run.class,
                kubernetesFacade.getNamespace(),
                new ListOptionsBuilder().withLimit(limit).build());
        return resources;
    }
}

package com.redhat.agogos.cli.commands.stage;

import com.redhat.agogos.cli.commands.AbstractListCommand;
import com.redhat.agogos.core.v1alpha1.Stage;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import picocli.CommandLine.Command;

import java.util.List;

@Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list stages")
public class StageListCommand extends AbstractListCommand<Stage> {

    @Override
    public List<Stage> getResources() {
        List<Stage> resources = kubernetesFacade.list(
                Stage.class,
                kubernetesFacade.getNamespace(),
                new ListOptionsBuilder().withLimit(limit).build());
        return resources;
    }
}

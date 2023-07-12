package com.redhat.agogos.cli.commands.trigger;

import com.redhat.agogos.cli.commands.AbstractListCommand;
import com.redhat.agogos.core.v1alpha1.triggers.Trigger;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import picocli.CommandLine.Command;

import java.util.List;

@Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list triggers")
class TriggerListCommand extends AbstractListCommand<Trigger> {

    @Override
    public List<Trigger> getResources() {
        List<Trigger> resources = kubernetesFacade.list(
                Trigger.class,
                kubernetesFacade.getNamespace(),
                new ListOptionsBuilder().withLimit(limit).build());
        return resources;
    }

}
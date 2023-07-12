package com.redhat.agogos.cli.commands.component;

import com.redhat.agogos.cli.commands.AbstractListCommand;
import com.redhat.agogos.core.v1alpha1.Component;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import picocli.CommandLine.Command;

import java.util.List;

@Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list components")
public class ComponentListCommand extends AbstractListCommand<Component> {

    @Override
    public List<Component> getResources() {
        List<Component> resources = kubernetesFacade.list(
                Component.class,
                kubernetesFacade.getNamespace(),
                new ListOptionsBuilder().withLimit(limit).build());
        return resources;
    }
}

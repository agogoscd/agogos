package com.redhat.agogos.cli.commands.group;

import com.redhat.agogos.cli.commands.AbstractListCommand;
import com.redhat.agogos.core.v1alpha1.Group;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import picocli.CommandLine.Command;

import java.util.List;

@Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list groups")
public class GroupListCommand extends AbstractListCommand<Group> {

    @Override
    public List<Group> getResources() {
        List<Group> resources = kubernetesFacade.list(
                Group.class,
                kubernetesFacade.getNamespace(),
                new ListOptionsBuilder().withLimit(limit).build());
        return resources;
    }
}

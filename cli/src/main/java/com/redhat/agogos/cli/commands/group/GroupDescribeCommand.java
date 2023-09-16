package com.redhat.agogos.cli.commands.group;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.core.v1alpha1.Group;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(mixinStandardHelpOptions = true, name = "describe", aliases = { "desc", "d" }, description = "describe group")
public class GroupDescribeCommand extends AbstractResourceSubcommand<Group> {
    @Parameters(index = "0", description = "Name of the Group")
    String name;

    @Override
    public Integer call() {
        Group group = kubernetesFacade.get(Group.class, kubernetesFacade.getNamespace(), name);
        return showResource(group);
    }
}

package com.redhat.agogos.cli.commands.group;

import com.redhat.agogos.cli.commands.AbstractResourceSubcommand;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Group;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.Map;
import java.util.UUID;

@Command(mixinStandardHelpOptions = true, name = "build", aliases = { "b" }, description = "build a group")
public class GroupBuildCommand extends AbstractResourceSubcommand<Group> {
    @Parameters(index = "0", description = "Name of the group to build.")
    String name;

    @Override
    public void run() {
        Map<String, String> labels = Map.of(
                Resource.GROUP.getResourceLabel(), name,
                Resource.getInstanceLabel(), UUID.randomUUID().toString());

        Group group = kubernetesFacade.get(Group.class, kubernetesFacade.getNamespace(), name);

        group.getSpec().getComponents().stream().forEach(component -> {
            Build build = new Build();
            build.getMetadata().setGenerateName(component + "-");
            build.getMetadata().setNamespace(kubernetesFacade.getNamespace());
            build.getMetadata().getLabels().putAll(labels);
            build.getSpec().setComponent(component);

            build = kubernetesFacade.create(build);
        });

        cli.run(GroupDescribeCommand.class, group.getMetadata().getName());
    }
}

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
        List<Builder> builders = getBuilders(kubernetesFacade.getNamespace());
        if (limit == 0 || builders.size() < limit) {
            builders.addAll(getBuilders(agogosEnvironment.getRunningNamespace()));
        }
        return limit == 0 ? builders : builders.subList(0, Math.toIntExact(limit));
    }

    @Override
    protected Integer printList(List<Builder> resources) {
        return printList(resources, true);
    }

    private List<Builder> getBuilders(String namespace) {
        return kubernetesFacade.list(
                Builder.class,
                namespace,
                new ListOptionsBuilder().withLimit(limit).build());
    }
}

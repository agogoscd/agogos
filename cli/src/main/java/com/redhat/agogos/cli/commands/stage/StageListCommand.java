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
        List<Stage> stages = getStages(kubernetesFacade.getNamespace());
        if (limit == 0 || stages.size() < limit) {
            stages.addAll(getStages(agogosEnvironment.getRunningNamespace()));
        }
        return limit == 0 ? stages : stages.subList(0, Math.toIntExact(limit));
    }

    @Override
    protected Integer printList(List<Stage> resources) {
        return printList(resources, true);
    }

    private List<Stage> getStages(String namespace) {
        return kubernetesFacade.list(
                Stage.class,
                namespace,
                new ListOptionsBuilder().withLimit(limit).build());
    }
}

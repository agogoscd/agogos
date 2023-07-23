package com.redhat.agogos.cli.commands.execution;

import com.redhat.agogos.cli.commands.AbstractListCommand;
import com.redhat.agogos.core.v1alpha1.Execution;
import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import picocli.CommandLine.Command;

import java.util.List;

@Command(mixinStandardHelpOptions = true, name = "list", aliases = { "l" }, description = "list executions")
public class ExecutionListCommand extends AbstractListCommand<Execution> {

    @Override
    public List<Execution> getResources() {
        List<Execution> resources = kubernetesFacade.list(
                Execution.class,
                kubernetesFacade.getNamespace(),
                new ListOptionsBuilder().withLimit(limit).build());
        return resources;
    }
}

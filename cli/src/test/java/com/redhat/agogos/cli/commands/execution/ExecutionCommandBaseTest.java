package com.redhat.agogos.cli.commands.execution;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import com.redhat.agogos.core.v1alpha1.Execution;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public abstract class ExecutionCommandBaseTest extends AbstractCommandTest {
    protected List<Execution> executions;

    @BeforeAll
    public void beforeAll() {
        executions = utils.loadTestResources(Execution.class, "commands/execution/executions.yml");
    }
}

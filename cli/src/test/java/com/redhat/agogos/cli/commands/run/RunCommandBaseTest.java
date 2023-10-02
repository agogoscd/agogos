package com.redhat.agogos.cli.commands.run;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import com.redhat.agogos.core.v1alpha1.Run;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public abstract class RunCommandBaseTest extends AbstractCommandTest {
    protected List<Run> runs;

    @BeforeAll
    public void beforeAll() {
        runs = utils.loadTestResources(Run.class, "commands/run/runs.yml");
    }
}

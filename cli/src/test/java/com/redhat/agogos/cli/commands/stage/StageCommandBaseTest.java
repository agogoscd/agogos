package com.redhat.agogos.cli.commands.stage;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import com.redhat.agogos.core.v1alpha1.Stage;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public abstract class StageCommandBaseTest extends AbstractCommandTest {
    protected List<Stage> stages;

    @BeforeAll
    public void beforeAll() {
        stages = utils.loadTestResources(Stage.class, "commands/stage/stages.yml");
    }
}

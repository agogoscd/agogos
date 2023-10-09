package com.redhat.agogos.cli.commands.trigger;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import com.redhat.agogos.core.v1alpha1.triggers.Trigger;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public abstract class TriggerCommandBaseTest extends AbstractCommandTest {
    protected List<Trigger> triggers;

    @BeforeAll
    public void beforeAll() {
        triggers = utils.loadTestResources(Trigger.class, "commands/trigger/triggers.yml");
    }
}

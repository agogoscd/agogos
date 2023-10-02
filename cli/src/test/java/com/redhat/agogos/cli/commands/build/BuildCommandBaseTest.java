package com.redhat.agogos.cli.commands.build;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import com.redhat.agogos.core.v1alpha1.Build;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public abstract class BuildCommandBaseTest extends AbstractCommandTest {
    protected List<Build> builds;

    @BeforeAll
    public void beforeAll() {
        builds = utils.loadTestResources(Build.class, "commands/build/builds.yml");
    }
}

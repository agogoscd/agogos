package com.redhat.agogos.cli.commands.component;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Component;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public abstract class ComponentCommandBaseTest extends AbstractCommandTest {
    protected List<Build> builds;
    protected List<Component> components;

    @BeforeAll
    public void beforeAll() {
        builds = utils.loadTestResources(Build.class, "commands/component/builds.yml");
        components = utils.loadTestResources(Component.class, "commands/component/components.yml");
    }
}

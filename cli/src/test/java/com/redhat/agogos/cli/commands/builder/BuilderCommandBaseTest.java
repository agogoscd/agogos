package com.redhat.agogos.cli.commands.builder;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import com.redhat.agogos.core.v1alpha1.Builder;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public abstract class BuilderCommandBaseTest extends AbstractCommandTest {
    protected List<Builder> builders;

    @BeforeAll
    public void beforeAll() {
        builders = utils.loadTestResources(Builder.class, "commands/builder/builders.yml");
    }
}

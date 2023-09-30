package com.redhat.agogos.cli.commands.group;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import com.redhat.agogos.core.v1alpha1.Execution;
import com.redhat.agogos.core.v1alpha1.Group;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

public abstract class GroupCommandBaseTest extends AbstractCommandTest {
    protected List<Group> groups;
    protected List<Execution> executions;

    @BeforeAll
    public void beforeAll() {
        groups = utils.loadTestResources(Group.class, "group/groups.yml");
        executions = utils.loadTestResources(Execution.class, "group/executions.yml");
    }
}

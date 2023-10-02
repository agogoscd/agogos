package com.redhat.agogos.cli.commands.group;

import com.redhat.agogos.core.v1alpha1.Group;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class GroupDescribeCommandTest extends GroupCommandBaseTest {

    @Test
    public void describeHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "group", "describe", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/group/describe-help.txt")));
    }

    @Test
    public void describeNoGroupSpecified() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "group", "describe");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(catcher.compareToStderr(utils.testResourceAsStringList("commands/group/describe-no-group.txt")));
    }

    @Test
    public void describeSpecificGroup() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Group.class, "namespace", "dummy-component-group-1"))
                .thenReturn(groups.get(0));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "group", "describe", "dummy-component-group-1");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdout(utils.testResourceAsStringList("commands/group/describe-specific-group.txt")));
    }

    @Test
    public void describeUnknownGroup() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Group.class, "namespace", "unknown-group"))
                .thenReturn(null);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "group", "describe", "unknown-group");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(
                catcher.compareToStderr(utils.testResourceAsStringList("commands/group/describe-unknown-group.txt")));
    }
}

package com.redhat.agogos.cli.commands.group;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.redhat.agogos.core.v1alpha1.Execution;
import com.redhat.agogos.core.v1alpha1.Group;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class GroupExecuteCommandTest extends GroupCommandBaseTest {

    @Test
    public void executionHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "group", "execute", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/group/execution-help.txt")));
    }

    @Test
    public void executionNoGroupSpecified() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "group", "execute");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(catcher.compareToStderr(utils.testResourceAsStringList("commands/group/execution-no-group.txt")));
    }

    @Test
    public void executionSpecificGroup() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Group.class, "namespace", "dummy-component-group-1"))
                .thenReturn(groups.get(0));
        Mockito.when(kubernetesFacadeMock.listNotEmpty(eq(Execution.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(executions.subList(0, 1));
        Mockito.when(kubernetesFacadeMock.get(Execution.class, "namespace", "dummy-component-group-1-6q8pp"))
                .thenReturn(executions.get(0));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "group", "execute", "dummy-component-group-1");
        catcher.logStdout();
        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdout(utils.testResourceAsStringList("commands/group/execution-specific-group.txt")));
    }

    @Test
    public void executionSpecificGroupExecutionNotFound() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Group.class, "namespace", "dummy-component-group-1"))
                .thenReturn(groups.get(0));
        Mockito.when(kubernetesFacadeMock.listNotEmpty(eq(Execution.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(executions.subList(0, 0));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "group", "execute", "dummy-component-group-1");

        Assertions.assertEquals(ExitCode.SOFTWARE, returnCode);
        Assertions.assertTrue(
                catcher.compareToStderrSanitized(
                        utils.testResourceAsStringList("commands/group/execution-execution-not-found.txt")));
    }

    @Test
    public void executionNotFoundGroup() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Group.class, "namespace", "not-found"))
                .thenReturn(null);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "group", "execute", "not-found");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(
                catcher.compareToStderr(utils.testResourceAsStringList("commands/group/execution-not-found-group.txt")));
    }
}

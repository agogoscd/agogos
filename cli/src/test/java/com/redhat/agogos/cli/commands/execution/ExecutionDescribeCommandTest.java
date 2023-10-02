package com.redhat.agogos.cli.commands.execution;

import com.redhat.agogos.core.v1alpha1.Execution;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class ExecutionDescribeCommandTest extends ExecutionCommandBaseTest {

    @Test
    public void describeHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "executions", "describe", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/execution/describe-help.txt")));
    }

    @Test
    public void describeNoExecutionSpecified() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "executions", "describe");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(
                catcher.compareToStderr(utils.testResourceAsStringList("commands/execution/describe-no-execution.txt")));
    }

    @Test
    public void describeSpecificExecution() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Execution.class, "namespace", "dummy-component-group-1-4dvh6"))
                .thenReturn(executions.get(0));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "executions", "describe", "dummy-component-group-1-4dvh6");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdout(utils.testResourceAsStringList("commands/execution/describe-specific-execution.txt")));
    }

    @Test
    public void describeRunningExecution() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Execution.class, "namespace", "dummy-component-group-1-xwts6"))
                .thenReturn(executions.get(2));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "executions", "describe", "dummy-component-group-1-xwts6");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdoutSanitized(
                        utils.testResourceAsStringList("commands/execution/describe-running-execution.txt")));
    }

    @Test
    public void describeFailedExecution() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Execution.class, "namespace", "dummy-component-group-1-f8n7t"))
                .thenReturn(executions.get(3));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "executions", "describe", "dummy-component-group-1-f8n7t");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdoutSanitized(
                        utils.testResourceAsStringList("commands/execution/describe-failed-execution.txt")));
    }

    @Test
    public void describeLastExecution() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(Execution.class, "namespace"))
                .thenReturn(executions);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "executions", "describe", "--last");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdout(utils.testResourceAsStringList("commands/execution/describe-last-execution.txt")));
    }
}

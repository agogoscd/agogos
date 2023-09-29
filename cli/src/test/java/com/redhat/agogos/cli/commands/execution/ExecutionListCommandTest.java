package com.redhat.agogos.cli.commands.execution;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.redhat.agogos.core.v1alpha1.Execution;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class ExecutionListCommandTest extends ExecutionCommandBaseTest {

    @Test
    public void listHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "executions", "list", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("execution/list-help.txt")));
    }

    @Test
    public void list() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(eq(Execution.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(executions);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "executions", "list");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("execution/list.txt")));
    }

    @Test
    public void listWithLimit() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(eq(Execution.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(executions.subList(0, 1));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "executions", "list", "--limit", "1");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("execution/list-with-limit.txt")));
    }
}

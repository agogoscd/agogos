package com.redhat.agogos.cli.commands.run;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.redhat.agogos.core.v1alpha1.Run;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class RunListCommandTest extends RunCommandBaseTest {

    @Test
    public void listHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "run", "list", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/run/list-help.txt")));
    }

    @Test
    public void list() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(eq(Run.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(runs);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "run", "list");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/run/list.txt")));
    }

    @Test
    public void listWithLimit() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(eq(Run.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(runs.subList(0, 3));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "run", "list", "--limit", "3");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/run/list-with-limit.txt")));
    }
}

package com.redhat.agogos.cli.commands.stage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.redhat.agogos.core.v1alpha1.Stage;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class StageListCommandTest extends StageCommandBaseTest {

    @Test
    public void listHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "stage", "list", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/stage/list-help.txt")));
    }

    @Test
    public void list() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(eq(Stage.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(stages);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "stage", "list");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/stage/list.txt")));
    }

    @Test
    public void listWithLimit() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(eq(Stage.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(stages.subList(0, 2));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "stage", "list", "--limit", "2");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/stage/list-with-limit.txt")));
    }
}

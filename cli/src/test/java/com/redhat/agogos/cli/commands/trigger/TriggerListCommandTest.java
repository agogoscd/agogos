package com.redhat.agogos.cli.commands.trigger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.redhat.agogos.core.v1alpha1.triggers.Trigger;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class TriggerListCommandTest extends TriggerCommandBaseTest {

    @Test
    public void listHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "trigger", "list", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/trigger/list-help.txt")));
    }

    @Test
    public void list() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(eq(Trigger.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(triggers);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "trigger", "list");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/trigger/list.txt")));
    }

    @Test
    public void listWithLimit() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(eq(Trigger.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(triggers.subList(0, 5));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "trigger", "list", "--limit", "2");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/trigger/list-with-limit.txt")));
    }
}

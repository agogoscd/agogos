package com.redhat.agogos.cli.commands.trigger;

import com.redhat.agogos.core.v1alpha1.triggers.Trigger;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class TriggerDescribeCommandTest extends TriggerCommandBaseTest {

    @Test
    public void describeHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "trigger", "describe", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/trigger/describe-help.txt")));
    }

    @Test
    public void describeNoTriggerSpecified() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "trigger", "describe");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(
                catcher.compareToStderr(utils.testResourceAsStringList("commands/trigger/describe-no-trigger.txt")));
    }

    @Test
    public void describeSpecificTrigger() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Trigger.class, "namespace", "cel-to-component"))
                .thenReturn(triggers.get(1));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "trigger", "describe", "cel-to-component");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdout(utils.testResourceAsStringList("commands/trigger/describe-specific-trigger.txt")));
    }

    @Test
    public void describeUnknownTrigger() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Trigger.class, "namespace", "unknown-trigger"))
                .thenReturn(null);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "trigger", "describe", "unknown-trigger");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(
                catcher.compareToStderr(utils.testResourceAsStringList("commands/trigger/describe-unknown-trigger.txt")));
    }
}

package com.redhat.agogos.cli.commands.stage;

import com.redhat.agogos.core.v1alpha1.Stage;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class StageDescribeCommandTest extends StageCommandBaseTest {

    @Test
    public void describeHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "stage", "describe", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/stage/describe-help.txt")));
    }

    @Test
    public void describeNoStageSpecified() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "stage", "describe");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(
                catcher.compareToStderr(utils.testResourceAsStringList("commands/stage/describe-no-stage.txt")));
    }

    @Test
    public void describeSpecificStage() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Stage.class, "namespace", "errata-stage-v1"))
                .thenReturn(stages.get(3));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "stage", "describe", "errata-stage-v1");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdout(utils.testResourceAsStringList("commands/stage/describe-specific-stage.txt")));
    }
}

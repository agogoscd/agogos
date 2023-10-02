package com.redhat.agogos.cli.commands.run;

import com.redhat.agogos.core.v1alpha1.Run;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class RunDescribeCommandTest extends RunCommandBaseTest {

    @Test
    public void describeHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "run", "describe", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/run/describe-help.txt")));
    }

    @Test
    public void describeNoRunSpecified() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "run", "describe");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(catcher.compareToStderr(utils.testResourceAsStringList("commands/run/describe-no-run.txt")));
    }

    @Test
    public void describeSpecificRun() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Run.class, "namespace", "dummy-pipeline-1-vztp7"))
                .thenReturn(runs.get(3));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "run", "describe", "dummy-pipeline-1-vztp7");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdout(utils.testResourceAsStringList("commands/run/describe-specific-run.txt")));
    }

    @Test
    public void describeLastRun() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(Run.class, "namespace"))
                .thenReturn(runs);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "run", "describe", "--last");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions
                .assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/run/describe-last-run.txt")));
    }
}

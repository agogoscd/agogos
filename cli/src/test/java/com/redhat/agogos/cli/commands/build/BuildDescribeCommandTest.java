package com.redhat.agogos.cli.commands.build;

import com.redhat.agogos.core.v1alpha1.Build;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class BuildDescribeCommandTest extends BuildCommandBaseTest {

    @Test
    public void describeHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "build", "describe", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/build/describe-help.txt")));
    }

    @Test
    public void describeNoBuildSpecified() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "build", "describe");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(catcher.compareToStderr(utils.testResourceAsStringList("commands/build/describe-no-build.txt")));
    }

    @Test
    public void describeSpecificBuild() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Build.class, "namespace", "dummy-component-dep-1-62k7v"))
                .thenReturn(builds.get(0));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "build", "describe", "dummy-component-dep-1-62k7v");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdout(utils.testResourceAsStringList("commands/build/describe-specific-build.txt")));
    }

    @Test
    public void describeLastBuild() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.list(Build.class, "namespace"))
                .thenReturn(builds);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "build", "describe", "--last");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions
                .assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/build/describe-last-build.txt")));
    }
}

package com.redhat.agogos.cli.commands.builder;

import com.redhat.agogos.core.v1alpha1.Builder;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class BuilderDescribeCommandTest extends BuilderCommandBaseTest {

    @Test
    public void describeHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "builder", "describe", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/builder/describe-help.txt")));
    }

    @Test
    public void describeNoBuilderSpecified() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "builder", "describe");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(
                catcher.compareToStderr(utils.testResourceAsStringList("commands/builder/describe-no-builder.txt")));
    }

    @Test
    public void describeSpecificBuilder() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Builder.class, "namespace", "dummy-v1"))
                .thenReturn(builders.get(0));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "builder", "describe", "dummy-v1");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdout(utils.testResourceAsStringList("commands/builder/describe-specific-builder.txt")));
    }
}

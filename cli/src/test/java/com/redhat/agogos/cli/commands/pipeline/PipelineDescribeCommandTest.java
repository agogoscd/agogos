package com.redhat.agogos.cli.commands.pipeline;

import com.redhat.agogos.core.v1alpha1.Pipeline;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class PipelineDescribeCommandTest extends PipelineCommandBaseTest {

    @Test
    public void describeHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "pipeline", "describe", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/pipeline/describe-help.txt")));
    }

    @Test
    public void describeNoPipelineSpecified() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "pipeline", "describe");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(
                catcher.compareToStderr(utils.testResourceAsStringList("commands/pipeline/describe-no-pipeline.txt")));
    }

    @Test
    public void describeSpecificPipeline() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Pipeline.class, "namespace", "dummy-group-3-pipeline-1"))
                .thenReturn(pipelines.get(0));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "pipeline", "describe", "dummy-group-3-pipeline-1");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdout(utils.testResourceAsStringList("commands/pipeline/describe-specific-pipeline.txt")));
    }

    @Test
    public void describeUnknownPipeline() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Pipeline.class, "namespace", "unknown-pipeline"))
                .thenReturn(null);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "pipeline", "describe", "unknown-pipeline");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(
                catcher.compareToStderr(utils.testResourceAsStringList("commands/pipeline/describe-unknown-pipeline.txt")));
    }
}

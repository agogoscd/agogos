package com.redhat.agogos.cli.commands.pipeline;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.redhat.agogos.core.v1alpha1.Pipeline;
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
public class PipelineRunCommandTest extends PipelineCommandBaseTest {

    @Test
    public void runHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "pipeline", "run", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("commands/pipeline/run-help.txt")));
    }

    @Test
    public void runNoPipelineSpecified() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "pipeline", "run");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(catcher.compareToStderr(utils.testResourceAsStringList("commands/pipeline/run-no-pipeline.txt")));
    }

    @Test
    public void runSpecificPipeline() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Pipeline.class, "namespace", "dummy-pipeline-1"))
                .thenReturn(pipelines.get(4));
        Mockito.when(kubernetesFacadeMock.listNotEmpty(eq(Run.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(runs.subList(3, 4));
        Mockito.when(kubernetesFacadeMock.get(Run.class, "namespace", "dummy-pipeline-1-vztp7"))
                .thenReturn(runs.get(3));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "pipeline", "run", "dummy-pipeline-1");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(
                catcher.compareToStdout(utils.testResourceAsStringList("commands/pipeline/run-specific-pipeline.txt")));
    }

    @Test
    public void runSpecificPipelineRunNotFound() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Pipeline.class, "namespace", "dummy-pipeline-1"))
                .thenReturn(pipelines.get(0));
        Mockito.when(kubernetesFacadeMock.listNotEmpty(eq(Run.class), eq("namespace"), any(ListOptions.class)))
                .thenReturn(runs.subList(0, 0));

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "pipeline", "run", "dummy-pipeline-1");

        Assertions.assertEquals(ExitCode.SOFTWARE, returnCode);
        Assertions.assertTrue(
                catcher.compareToStderrSanitized(utils.testResourceAsStringList("commands/pipeline/run-run-not-found.txt")));
    }

    @Test
    public void runNotFoundPipeline() throws Exception {
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(kubernetesFacadeMock.get(Pipeline.class, "namespace", "not-found"))
                .thenReturn(null);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "pipeline", "run", "not-found");

        Assertions.assertEquals(ExitCode.USAGE, returnCode);
        Assertions.assertTrue(
                catcher.compareToStderrSanitized(
                        utils.testResourceAsStringList("commands/pipeline/run-not-found-pipeline.txt")));
    }
}

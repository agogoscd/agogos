package com.redhat.agogos.cli.commands.pipeline;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.commands.CommandTest;
import com.redhat.agogos.v1alpha1.Run;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NamespaceableResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import picocli.CommandLine;

@QuarkusTest
public class PipelineRunCommandTest extends CommandTest {

    @InjectMock(convertScopes = true, returnsDeepMocks = true)
    KubernetesClient kubernetesClientMock;

    @InjectMock
    CLI cliMock;

    @InjectMocks
    PipelineRunCommand pipelineRunCommand = new PipelineRunCommand();

    @Test
    public void callsResourceAndAnotherCommand() throws Exception {
        NamespaceableResource<Run> nsRun = (NamespaceableResource<Run>) Mockito.mock(NamespaceableResource.class);
        Mockito.when(kubernetesClientMock.resource(Mockito.any(Run.class))).thenReturn(nsRun);
        Mockito.when(nsRun.create()).thenReturn(new Run());
        Mockito.when(cliMock.run(Mockito.any(), Mockito.any())).thenReturn(0);

        CommandLine cmd = new CommandLine(pipelineRunCommand);

        int returnCode = cmd.execute("pipeline");

        Assertions.assertEquals(0, returnCode);
    }
}

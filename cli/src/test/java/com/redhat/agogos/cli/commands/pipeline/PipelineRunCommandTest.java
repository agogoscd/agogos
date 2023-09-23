package com.redhat.agogos.cli.commands.pipeline;

import static org.mockito.Mockito.times;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.commands.AbstractCommandTest;
import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.Run;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.MockitoConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.HashMap;

@QuarkusTest
public class PipelineRunCommandTest extends AbstractCommandTest {

    @InjectMock
    KubernetesFacade kubernetesFacadeMock;

    @MockitoConfig(convertScopes = true)
    @InjectMock
    KubernetesSerialization objectMapperMock;

    @InjectMock
    CLI cliMock;

    @InjectMocks
    PipelineRunCommand pipelineRunCommand = new PipelineRunCommand();

    @Test
    public void callsResourceAndAnotherCommand() throws Exception {
        Mockito.when(kubernetesFacadeMock.get(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new Pipeline());
        Mockito.when(objectMapperMock.asJson(Mockito.any()))
                .thenReturn("");
        Mockito.when(kubernetesFacadeMock.unmarshal(Mockito.any(), Mockito.any()))
                .thenReturn(new HashMap<String, Object>());
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("namespace");
        Mockito.when(
                kubernetesFacadeMock.listNotEmpty(Mockito.any(), Mockito.any(String.class), Mockito.any(ListOptions.class)))
                .thenReturn(Arrays.asList(new Run()));
        Mockito.when(cliMock.run(Mockito.any(), Mockito.any())).thenReturn(0);

        CommandLine cmd = new CommandLine(pipelineRunCommand);

        int returnCode = cmd.execute("pipeline");

        Assertions.assertEquals(0, returnCode);
        Mockito.verify(kubernetesFacadeMock).listNotEmpty(Mockito.any(), Mockito.any(String.class),
                Mockito.any(ListOptions.class));
        Mockito.verify(kubernetesFacadeMock, times(2)).getNamespace();
    }
}

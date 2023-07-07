package com.redhat.agogos.cli.commands.pipeline;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.commands.AbstractCommandTest;
import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.v1alpha1.Run;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import picocli.CommandLine;

@QuarkusTest
public class PipelineRunCommandTest extends AbstractCommandTest {

    @InjectMock
    KubernetesFacade kubernetesFacadeMock;

    @InjectMock
    CLI cliMock;

    @InjectMocks
    PipelineRunCommand pipelineRunCommand = new PipelineRunCommand();

    @Test
    public void callsResourceAndAnotherCommand() throws Exception {
        Mockito.when(kubernetesFacadeMock.create(Mockito.any(Run.class))).thenReturn(new Run());
        Mockito.when(cliMock.run(Mockito.any(), Mockito.any())).thenReturn(0);

        CommandLine cmd = new CommandLine(pipelineRunCommand);

        int returnCode = cmd.execute("pipeline");

        Assertions.assertEquals(0, returnCode);
        Mockito.verify(kubernetesFacadeMock).create(Mockito.any(Run.class));
        Mockito.verify(kubernetesFacadeMock).getNamespace();
    }
}

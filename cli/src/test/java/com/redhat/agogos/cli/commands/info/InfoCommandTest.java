package com.redhat.agogos.cli.commands.info;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import com.redhat.agogos.core.KubernetesFacade;
import io.fabric8.kubernetes.client.VersionInfo;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

@QuarkusTest
public class InfoCommandTest extends AbstractCommandTest {

    @InjectMock
    KubernetesFacade kubernetesFacadeMock;

    @InjectMocks
    InfoCommand infoCommand = new InfoCommand();

    @Test
    public void ensureDataIsPrinted() throws Exception {
        StringWriter sw = new StringWriter();

        VersionInfo versionInfo = new VersionInfo.Builder().withMajor("x").withMinor("y").build();
        Mockito.when(kubernetesFacadeMock.getMasterUrl()).thenReturn(new URL("https://localhost:6443"));
        Mockito.when(kubernetesFacadeMock.getNamespace()).thenReturn("default");
        Mockito.when(kubernetesFacadeMock.getKubernetesVersion()).thenReturn(versionInfo);

        CommandLine cmd = new CommandLine(infoCommand);
        cmd.setOut(new PrintWriter(sw));

        int returnCode = cmd.execute();

        Assertions.assertEquals(0, returnCode);
        String output = sw.toString();
        Assertions.assertTrue(output.contains("https://localhost:6443"));
        Assertions.assertTrue(output.contains("default"));
        Assertions.assertTrue(output.contains("x"));
        Assertions.assertTrue(output.contains("y"));
    }
}

package com.redhat.agogos.cli.commands.info;

import com.redhat.agogos.cli.commands.CommandTest;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

@QuarkusTest
public class InfoCommandTest extends CommandTest {

    @InjectMock(convertScopes = true)
    KubernetesClient kubernetesClientMock;

    @InjectMocks
    InfoCommand infoCommand = new InfoCommand();

    @Test
    public void ensureDataIsPrinted() throws Exception {
        StringWriter sw = new StringWriter();

        Mockito
                .when(kubernetesClientMock.getMasterUrl())
                .thenReturn(new URL("https://localhost:6443"));
        Mockito
                .when(kubernetesClientMock.getNamespace())
                .thenReturn("default");
        VersionInfo versionInfo = new VersionInfo.Builder()
                .withMajor("x")
                .withMinor("y")
                .build();
        Mockito
                .when(kubernetesClientMock.getKubernetesVersion())
                .thenReturn(versionInfo);

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

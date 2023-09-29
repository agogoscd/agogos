package com.redhat.agogos.cli.commands.info;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import io.fabric8.kubernetes.client.VersionInfo;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import picocli.CommandLine.ExitCode;

import java.net.URL;

@QuarkusTest
public class InfoCommandTest extends AbstractCommandTest {

    @Test
    public void infoHelp() throws Exception {
        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "info", "--help");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("info/help.txt")));
    }

    @Test
    public void infoOutput() throws Exception {
        VersionInfo versionInfo = new VersionInfo.Builder().withMajor("x").withMinor("y").build();
        Mockito.when(kubernetesFacadeMock.getMasterUrl())
                .thenReturn(new URL("https://localhost:6443"));
        Mockito.when(kubernetesFacadeMock.getNamespace())
                .thenReturn("default");
        Mockito.when(kubernetesFacadeMock.getKubernetesVersion())
                .thenReturn(versionInfo);

        int returnCode = cli.run(catcher.getOut(), catcher.getErr(), "info");

        Assertions.assertEquals(ExitCode.OK, returnCode);
        Assertions.assertTrue(catcher.compareToStdout(utils.testResourceAsStringList("info/info-output.txt")));
    }
}

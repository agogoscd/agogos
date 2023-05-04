package com.redhat.agogos.cli.commands.info;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.redhat.agogos.cli.commands.AbstractCommandTest;
import io.fabric8.kubernetes.client.VersionInfo;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;

@QuarkusTest
public class InfoCommandTest extends AbstractCommandTest {

    @Test
    @DisplayName("Should display information")
    void shouldDisplayInformation() throws Exception {
        when(mockClient.getKubernetesVersion())
                .thenReturn(new VersionInfo.Builder().withMajor("1").withMinor("26").build());
        when(mockClient.getMasterUrl())
                .thenReturn(new URL("https://127.0.0.1:8443"));
        when(mockClient.getNamespace())
                .thenReturn("default");

        int exitCode = cli.run(catcher.getOut(), catcher.getErr(), "info");

        assertEquals(0, exitCode); // Invalid output
        System.out.println(catcher.stdoutMessages());
        assertTrue(catcher.stdoutContains("Cluster URL:\t\thttps://127.0.0.1:8443"));
        assertTrue(catcher.stdoutContains("Namespace:\t\tdefault"));
        assertTrue(catcher.stdoutContains("Kubernetes version:\t1.26"));
    }
}

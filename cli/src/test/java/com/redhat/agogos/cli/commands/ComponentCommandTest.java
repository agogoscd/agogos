package com.redhat.agogos.cli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.test.InMemoryOutputCatcher;
import com.redhat.agogos.test.KubernetesTestServerSetup;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
public class ComponentCommandTest {

    @KubernetesTestServer
    KubernetesServer mockServer;

    @Inject
    CLI cli;

    InMemoryOutputCatcher catcher = new InMemoryOutputCatcher();

    @BeforeEach
    void setup() {
        catcher.reset();
    }

    @Test
    @DisplayName("Should not build unknown component")
    void shouldNotBuildUnknownComponent() throws Exception {
        mockServer.expect().post().withPath("/apis/agogos.redhat.com/v1alpha1/namespaces/test/builds")
                .andReturn(Status.BAD_REQUEST.getStatusCode(),
                        new StatusBuilder()
                                .withCode(Status.BAD_REQUEST.getStatusCode())
                                .withMessage("Component 'test-component' does not exist in namespace 'default'")
                                .build())
                .always();
        int exitCode = cli.run(catcher.getOut(), catcher.getErr(), "component", "build", "test-component");

        assertEquals(1, exitCode); // Invalid output
        assertTrue(catcher.stderrContains("🛑 Oops, an error occurred!"));
        assertTrue(catcher.stderrContains("Component 'test-component' does not exist in namespace 'default'"));
    }
}

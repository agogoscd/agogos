package com.redhat.agogos.cli.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.InMemoryLogHandler;
import com.redhat.agogos.cli.CLI;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import javax.inject.Inject;
import org.jboss.logmanager.LogContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
@QuarkusTest
public class InstallCommandTest {

    @KubernetesTestServer
    KubernetesServer mockServer;

    @Inject
    CLI cli;

    @Inject
    ObjectMapper objectMapper;

    InMemoryLogHandler handler;

    @BeforeEach
    void registerHandler() {
        handler = new InMemoryLogHandler();
        LogContext.getLogContext().getLogger("").addHandler(handler);
    }

    @AfterEach
    void deregisterHandler() {
        handler.getRecords().clear();
        LogContext.getLogContext().getLogger("").removeHandler(handler);
    }

    @Test
    @DisplayName("Should install 'local' profile")
    void shouldInstallLocalProfile() throws Exception {
        assertEquals(0, handler.getRecords().size());

        int exitCode = cli.run("adm", "install", "-p", "local");

        assertEquals(0, exitCode);

        assertTrue(handler.contains("💻 Selected profile: local"));
        assertTrue(handler.contains("✅ Tekton v0.22.0 installed"));
        assertTrue(handler.contains("✅ Tekton Triggers v0.12.1 installed"));
        assertTrue(handler.contains("✅ Knative Eventing v0.20.4 installed"));
        assertTrue(handler.contains("✅ Agogos CRDs installed"));
        assertTrue(handler.contains("✅ Agogos core resources installed"));
        assertTrue(handler.contains("✅ Agogos Webhook installed"));
        assertTrue(handler.contains("✅ Agogos Operator installed"));
    }

    @Test
    @DisplayName("Should install 'dev' profile")
    void shouldInstallDevProfile() throws Exception {
        assertEquals(0, handler.getRecords().size());

        int exitCode = cli.run("adm", "install", "-p", "dev");

        assertEquals(0, exitCode);

        assertTrue(handler.contains("💻 Selected profile: dev"));
        assertTrue(handler.contains("✅ Tekton v0.22.0 installed"));
        assertTrue(handler.contains("✅ Tekton Triggers v0.12.1 installed"));
        assertTrue(handler.contains("✅ Knative Eventing v0.20.4 installed"));
        assertTrue(handler.contains("✅ Agogos CRDs installed"));
        assertTrue(handler.contains("✅ Agogos core resources installed"));
    }
}

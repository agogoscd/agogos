package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.ResourceLoader;
import com.redhat.agogos.test.InMemoryOutputCatcher;
import com.redhat.agogos.test.KubernetesTestServerSetup;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
public abstract class AbstractCommandTest {

    @Inject
    protected ResourceLoader resourceLoader;

    @KubernetesTestServer
    protected KubernetesServer mockServer;

    @Inject
    protected CLI cli;

    protected InMemoryOutputCatcher catcher = new InMemoryOutputCatcher();

    @BeforeEach
    protected void setup() {
        catcher.reset();
    }

    AutoCloseable mocks;

    @BeforeEach
    void openMocks() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void closeMocks() throws Exception {
        mocks.close();
    }
}

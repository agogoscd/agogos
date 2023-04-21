package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.ResourceLoader;
import com.redhat.agogos.test.InMemoryOutputCatcher;
import com.redhat.agogos.test.KubernetesTestServerSetup;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import org.junit.jupiter.api.BeforeEach;

import javax.inject.Inject;

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
}

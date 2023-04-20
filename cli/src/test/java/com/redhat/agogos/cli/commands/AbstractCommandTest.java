package com.redhat.agogos.cli.commands;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.ResourceLoader;
import com.redhat.agogos.test.InMemoryOutputCatcher;
import com.redhat.agogos.test.KubernetesTestServerSetup;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

@WithKubernetesTestServer(setup = KubernetesTestServerSetup.class)
public abstract class AbstractCommandTest {

    @Inject
    ResourceLoader resourceLoader;

    @KubernetesTestServer
    KubernetesServer mockServer;
        
    @Inject
    CLI cli;

    InMemoryOutputCatcher catcher = new InMemoryOutputCatcher();

    @BeforeEach
    void setup() {
        catcher.reset();
    }

}

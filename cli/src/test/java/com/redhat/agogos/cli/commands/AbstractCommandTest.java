package com.redhat.agogos.cli.commands;

import com.redhat.agogos.cli.CLI;
import com.redhat.agogos.cli.ResourceLoader;
import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.test.InMemoryOutputCatcher;
import com.redhat.agogos.test.ResourceUtils;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.mockito.MockitoConfig;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

public abstract class AbstractCommandTest {

    @Inject
    protected ResourceLoader resourceLoader;

    @Inject
    protected CLI cli;

    @Inject
    protected ResourceUtils utils;

    @MockitoConfig(convertScopes = true)
    @InjectMock
    protected KubernetesClient kubernetesClientMock;

    @InjectMock
    protected KubernetesFacade kubernetesFacadeMock;

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

package com.redhat.agogos.test;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import jakarta.inject.Inject;

import java.util.function.Consumer;

/**
 * <p>
 * A custom class responsible for mocking the <code>/openapi/v2</code> context
 * path on the server and returning mocked OpenAPI data.
 * </p>
 * 
 * <p>
 * This is done globally, so every use of the mocked Kubernetes server will
 * be able to make use of it.
 * </p>
 */
public class KubernetesTestServerSetup implements Consumer<KubernetesServer> {

    @Inject
    ResourceUtils utils;

    static final String MOCKED_RESOURCE_NAME = "openapi.json";
    static final String PATH = "/openapi/v2";
    final String OPENAPI_DATA = utils.testResourceAsString(MOCKED_RESOURCE_NAME);

    @Override
    public void accept(KubernetesServer server) {
        server.expect().get().withPath(PATH)
                .andReturn(200, OPENAPI_DATA).always();

    }
}

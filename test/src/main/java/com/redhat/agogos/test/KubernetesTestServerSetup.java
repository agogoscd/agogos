package com.redhat.agogos.test;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;

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

    final static String MOCKED_RESOURCE_NAME = "openapi.json";
    final static String PATH = "/openapi/v2";
    static final String OPENAPI_DATA = ResourceUtils.testResourceAsString(MOCKED_RESOURCE_NAME);

    @Override
    public void accept(KubernetesServer server) {
        server.expect().get().withPath(PATH)
                .andReturn(200, OPENAPI_DATA).always();

    }
}

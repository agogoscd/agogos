package com.redhat.agogos.cli.test;

import io.fabric8.kubernetes.client.server.mock.KubernetesCrudDispatcher;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import java.io.IOException;
import java.util.function.Consumer;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

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

    static class AgogosCrudDispatcher extends KubernetesCrudDispatcher {

        final static String MOCKED_RESOURCE_NAME = "openapi.json";
        final static String PATH = "/openapi/v2";

        /**
         * Complete JSON OpenAPI schema.
         */
        static String OPENAPI_DATA = null;

        static {
            try {
                OPENAPI_DATA = new String(
                        Thread.currentThread().getContextClassLoader().getResourceAsStream(MOCKED_RESOURCE_NAME)
                                .readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public synchronized MockResponse dispatch(RecordedRequest request) {
            if (request.getPath().equals(PATH)) {
                return new MockResponse().setBody(OPENAPI_DATA);
            }

            return super.dispatch(request);
        }
    }

    @Override
    public void accept(KubernetesServer server) {
        server.getMockServer().setDispatcher(new AgogosCrudDispatcher());
    }
}

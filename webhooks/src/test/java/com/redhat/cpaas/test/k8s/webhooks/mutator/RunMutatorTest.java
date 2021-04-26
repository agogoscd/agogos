package com.redhat.cpaas.test.k8s.webhooks.mutator;

import com.redhat.agogos.errors.MissingResourceException;
import com.redhat.agogos.k8s.client.PipelineClient;
import com.redhat.agogos.k8s.webhooks.WebhookHandler;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.cpaas.test.TestResources;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.io.IOException;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
@TestHTTPEndpoint(WebhookHandler.class)
public class RunMutatorTest {

    @InjectMock
    PipelineClient pipelineClient;

    private static String admissionReview;

    @BeforeAll
    static void beforeAll() throws IOException {
        admissionReview = TestResources.asString("admission-review-run.json");
    }

    @Test
    @DisplayName("Should allow the request")
    public void testAllow() {
        Pipeline pipeline = new Pipeline();
        pipeline.getMetadata().setName("p1");
        pipeline.getMetadata().setNamespace("default");
        pipeline.getMetadata().setUid("1asdasd23rwfawef");

        Mockito.when(pipelineClient.getByName("p1", "default")).thenReturn(pipeline);

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/mutate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(true))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"));
    }

    @Test
    @DisplayName("Should fail request because Pipeline does not exist")
    public void testFailMissingPipeline() {
        Mockito.when(pipelineClient.getByName("p1", "default")).thenThrow(new MissingResourceException("Missing!"));

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/mutate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(false))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"));
    }
}

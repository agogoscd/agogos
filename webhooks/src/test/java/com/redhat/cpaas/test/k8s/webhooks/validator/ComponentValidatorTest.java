package com.redhat.cpaas.test.k8s.webhooks.validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.cpaas.k8s.client.StageResourceClient;
import com.redhat.cpaas.k8s.webhooks.WebhookHandler;
import com.redhat.cpaas.v1alpha1.AbstractStage.Phase;
import com.redhat.cpaas.v1alpha1.StageResource;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@QuarkusTest
@TestHTTPEndpoint(WebhookHandler.class)
public class ComponentValidatorTest {

    @InjectMock
    StageResourceClient stageResourceClient;

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void validateCorrect() throws IOException {
        InputStream review = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("review-component.json");

        // Mock the stage validation to return a stage
        Mockito.when(stageResourceClient.getByName("maven", Phase.BUILD)).thenReturn(new StageResource());

        RestAssured.given().when().request().contentType(ContentType.JSON).body(review).post("/validate").then()
                .statusCode(200).body("response.allowed", CoreMatchers.equalTo(true))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"));

    }

    @Test
    public void validateInvalidBuilder() throws IOException {
        InputStream review = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("review-component.json");

        // Mock the stage validation to not fnd a stage
        Mockito.when(stageResourceClient.getByName("maven", Phase.BUILD)).thenReturn(null);

        RestAssured.given().when().request().contentType(ContentType.JSON).body(review).post("/validate").then()
                .statusCode(200).body("response.allowed", CoreMatchers.equalTo(false))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validateCorrectBuilderData() throws IOException {
        InputStream review = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("review-component.json");

        InputStream schema = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("openapi-schema-valid.json");

        StageResource stage = new StageResource();
        stage.getSpec().getSchema().getOpenAPIV3Schema().putAll(objectMapper.readValue(schema, Map.class));

        Mockito.when(stageResourceClient.getByName("maven", Phase.BUILD)).thenReturn(stage);

        RestAssured.given().when().request().contentType(ContentType.JSON).body(review).post("/validate").then()
                .statusCode(200).body("response.allowed", CoreMatchers.equalTo(true))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validateIncorrectBuilderData() throws IOException {
        InputStream review = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("review-component.json");

        InputStream schema = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("openapi-schema-invalid.json");

        StageResource stage = new StageResource();
        stage.getSpec().getSchema().getOpenAPIV3Schema().putAll(objectMapper.readValue(schema, Map.class));

        Mockito.when(stageResourceClient.getByName("maven", Phase.BUILD)).thenReturn(stage);

        RestAssured.given().when().request().contentType(ContentType.JSON).body(review).post("/validate").then()
                .statusCode(200).body("response.allowed", CoreMatchers.equalTo(false))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"))
                .body("response.status.message", CoreMatchers.equalTo(
                        "Component definition 'default/component-name' is not valid: [Field 'someNonExistingKey' is required]"));
    }
}

package com.redhat.cpaas.test.k8s.webhooks.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.k8s.client.BuilderClient;
import com.redhat.agogos.k8s.webhooks.WebhookHandler;
import com.redhat.agogos.v1alpha1.Builder;
import com.redhat.cpaas.test.TestResources;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.io.IOException;
import javax.inject.Inject;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
@TestHTTPEndpoint(WebhookHandler.class)
public class ComponentValidatorTest {

    @InjectMock
    BuilderClient builderClient;

    @Inject
    ObjectMapper objectMapper;

    private static String admissionReview;

    @BeforeAll
    static void beforeAll() throws IOException {
        admissionReview = TestResources.asString("admission-review-component.json");
    }

    @Test
    @DisplayName("Validate correct review request")
    public void validateCorrect() throws IOException {
        // Mock the stage validation to return a stage
        Mockito.when(builderClient.getByName("maven")).thenReturn(new Builder());

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(true))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"));

    }

    @Test
    @DisplayName("Validate review request for a component with non-existing builder")
    public void validateInvalidBuilder() throws IOException {
        // Mock the stage validation to not find a stage
        Mockito.when(builderClient.getByName("maven")).thenReturn(null);

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(false))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"));
    }

    @Test
    @DisplayName("Validate valid data passed to builder")
    public void validateCorrectBuilderData() throws IOException {
        Builder builder = new Builder();
        builder.getSpec().getSchema().getOpenAPIV3Schema().putAll(TestResources.asMap("openapi-schema-valid.json"));

        Mockito.when(builderClient.getByName("maven")).thenReturn(builder);

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(true))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"));
    }

    @Test
    @DisplayName("Validate invalid data passed to builder")
    public void validateIncorrectBuilderData() throws IOException {
        Builder builder = new Builder();
        builder.getSpec().getSchema().getOpenAPIV3Schema().putAll(TestResources.asMap("openapi-schema-invalid.json"));

        Mockito.when(builderClient.getByName("maven")).thenReturn(builder);

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(false))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"))
                .body("response.status.message", CoreMatchers.equalTo(
                        "Component definition 'default/component-name' is not valid: [Field 'someNonExistingKey' is required]"));
    }
}

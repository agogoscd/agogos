package com.redhat.cpaas.test.k8s.webhooks.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.agogos.k8s.webhooks.WebhookHandler;
import com.redhat.agogos.test.CRDTestServerSetup;
import com.redhat.agogos.v1alpha1.Builder;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.cpaas.test.k8s.webhooks.validator.ComponentValidatorTest.ComponentValidatorTestServerSetup;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReviewBuilder;
import io.fabric8.kubernetes.api.model.authentication.UserInfo;
import io.fabric8.kubernetes.api.model.authentication.UserInfoBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer(setup = ComponentValidatorTestServerSetup.class)
@QuarkusTest
@TestHTTPEndpoint(WebhookHandler.class)
public class ComponentValidatorTest {

    public static class ComponentValidatorTestServerSetup extends CRDTestServerSetup {
        @Override
        public void accept(KubernetesServer server) {
            super.accept(server);

            Builder builder = new Builder("maven");
            Map<Object, Object> schema = builder.getSpec().getSchema().getOpenAPIV3Schema();

            schema.put("type", "object");
            schema.put("required", List.of("someKey"));
            schema.put("properties", Map.of("type", "string"));

            server.getClient().customResources(Builder.class).create(builder);
        }
    }

    @Inject
    ObjectMapper objectMapper;

    Component component;

    AdmissionReview admissionReview;

    @BeforeEach
    void beforeEach() {
        component = new Component("component-name");
        component.getMetadata().setNamespace("default");
        component.getSpec().getBuilderRef().put("name", "maven");

        UserInfo userInfo = new UserInfoBuilder().withUsername("minikube-user").withGroups("system:authenticated").build();

        AdmissionRequest admissionRequest = new AdmissionRequest();
        admissionRequest.setObject(component);
        admissionRequest.setUserInfo(userInfo);
        admissionRequest.setUid("bc1c421c-412c-40ae-86e3-52bc51b961a4");

        admissionReview = new AdmissionReviewBuilder().withRequest(admissionRequest).build();
    }

    @Test
    @DisplayName("Validate review request for a component with non-existing builder")
    public void validateInvalidBuilder() throws IOException {
        component.getSpec().getBuilderRef().put("name", "doesntexist");

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(false))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"))
                .body("response.status.message", CoreMatchers.equalTo(
                        "Selected builder 'doesntexist' is not registered in the system"));
    }

    @Test
    @DisplayName("Validate valid data passed to builder")
    public void validateCorrectBuilderData() throws IOException {
        component.getSpec().getData().put("someKey", "some allowed content");

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(true))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"));
    }

    @Test
    @DisplayName("Validate invalid data passed to builder")
    public void validateIncorrectBuilderData() throws IOException {
        Component component = (Component) admissionReview.getRequest().getObject();
        component.getSpec().getData().put("invalid", "data");

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(false))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"))
                .body("response.status.message", CoreMatchers.equalTo(
                        "Component definition 'default/component-name' is not valid: [Field 'someKey' is required]"));
    }
}

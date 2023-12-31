package com.redhat.agogos.webhooks.k8s.validator;

import com.redhat.agogos.core.v1alpha1.Builder;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.Stage;
import com.redhat.agogos.core.v1alpha1.StageEntry;
import com.redhat.agogos.test.CRDTestServerSetup;
import com.redhat.agogos.webhooks.k8s.WebhookHandler;
import com.redhat.agogos.webhooks.k8s.validator.ComponentValidatorTest.ComponentValidatorTestServerSetup;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReviewBuilder;
import io.fabric8.kubernetes.api.model.authentication.UserInfo;
import io.fabric8.kubernetes.api.model.authentication.UserInfoBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.tekton.pipeline.v1beta1.ParamSpec;
import io.fabric8.tekton.pipeline.v1beta1.ParamSpecBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ParamValueBuilder;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskBuilder;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Disabled("Enable when https://github.com/fabric8io/kubernetes-client/issues/5293 is released")
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

            server.getClient().resources(Builder.class).resource(builder).create();

            ParamSpec requiredParamSpec = new ParamSpecBuilder().withName("git-clone-param").build();
            ParamSpec optionalParamSpec = new ParamSpecBuilder().withName("git-clone-param-optional")
                    .withDefault(new ParamValueBuilder().withStringVal("default").build()).build();

            Task gitTask = new TaskBuilder().withNewMetadata().withName("git-clone").endMetadata().withNewSpec()
                    .withParams(requiredParamSpec, optionalParamSpec)
                    .endSpec().build();
            server.getClient().resources(Task.class).inNamespace("default").resource(gitTask).create();

            Stage stage = new Stage();
            stage.getSpec().getTaskRef().setName("git-clone");
            stage.getSpec().getSchema().getOpenAPIV3Schema().put("git-clone-param-optional",
                    Map.of("type", "object", "properties", Map.of("id", Map.of("type", "string"))));

            server.getClient().resources(Stage.class).inNamespace("default").resource(stage).create();
        }
    }

    Component component;

    AdmissionReview admissionReview;

    @BeforeEach
    void beforeEach() {
        component = new Component("component-name");
        component.getMetadata().setNamespace("default");
        component.getSpec().getBuild().getBuilderRef().setName("maven");

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
        component.getSpec().getBuild().getBuilderRef().setName("doesntexist");

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(false))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"))
                .body("response.status.message", CoreMatchers.equalTo(
                        "Selected builder 'doesntexist' is not registered in the system"));
    }

    @Test
    @DisplayName("Validate valid data passed to builder")
    public void validateCorrectBuilderData() throws IOException {
        component.getSpec().getBuild().getParams().put("someKey", "some allowed content");

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(true))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"));
    }

    @Test
    @DisplayName("Validate invalid data passed to builder")
    public void validateIncorrectBuilderData() throws IOException {
        Component component = (Component) admissionReview.getRequest().getObject();
        component.getSpec().getBuild().getParams().put("invalid", "data");

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(false))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"))
                .body("response.status.message", CoreMatchers.equalTo(
                        "Component definition 'default/component-name' is not valid: [Field 'someKey' is required]"));
    }

    @Test
    @DisplayName("Validate non-existing handler")
    public void validateNonExistingHandler() throws IOException {
        StageEntry entry = new StageEntry();
        entry.getStageRef().setName("doesnotexist");

        Component component = (Component) admissionReview.getRequest().getObject();
        component.getSpec().getPre().add(entry);
        component.getSpec().getBuild().getParams().put("someKey", "some allowed content");

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(false))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"))
                .body("response.status.message", CoreMatchers.equalTo(
                        "Component definition 'default/component-name' is not valid: specified Handler 'doesnotexist' does not exist in the system"));
    }

    @Test
    @DisplayName("Validate required stage params")
    public void validateMismatchedHandlerParams() throws IOException {
        StageEntry entry = new StageEntry();
        entry.getStageRef().setName("git-v1");

        Component component = (Component) admissionReview.getRequest().getObject();
        component.getSpec().getPre().add(entry);
        component.getSpec().getBuild().getParams().put("someKey", "some allowed content");

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(false))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"))
                .body("response.status.message", CoreMatchers.equalTo(
                        "Missing parameters in Handler 'git-v1': following parameters are required to be defined:: [git-clone-param]"));
    }

    @Test
    @DisplayName("Validate unknown handler params")
    public void validateUnknownHandlerParams() throws IOException {
        StageEntry entry = new StageEntry();
        entry.getStageRef().setName("git-v1");
        entry.getConfig().put("git-clone-param", "some content");
        entry.getConfig().put("doesnotexist", "some content");

        Component component = (Component) admissionReview.getRequest().getObject();
        component.getSpec().getPre().add(entry);
        component.getSpec().getBuild().getParams().put("someKey", "some content");

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(false))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"))
                .body("response.status.message", CoreMatchers.equalTo(
                        "Parameter mismatch in Handler 'git-v1': following parameters do not exist in Tekton Task 'git-clone': [doesnotexist]"));
    }

    @Test
    @DisplayName("Validate stage param with schema")
    public void validateHandlerParamWithSchema() throws IOException {
        StageEntry entry = new StageEntry();
        entry.getStageRef().setName("git-v1");
        entry.getConfig().put("git-clone-param", "some content");
        entry.getConfig().put("git-clone-param-optional", "some content");

        Component component = (Component) admissionReview.getRequest().getObject();
        component.getSpec().getPre().add(entry);
        component.getSpec().getBuild().getParams().put("someKey", "some content");

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(false))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"))
                .body("response.status.message", CoreMatchers.equalTo(
                        "Component 'default/component-name', Handler 'default/git-v1' parameter 'git-clone-param-optional' is not valid: [Type expected 'object', found 'string']"));
    }

    @Test
    @DisplayName("Validate correct stage")
    public void validateHandler() throws IOException {
        StageEntry entry = new StageEntry();
        entry.getStageRef().setName("git-v1");
        entry.getConfig().put("git-clone-param", "some content");
        entry.getConfig().put("git-clone-param-optional", Map.of("id", "asdasd"));

        Component component = (Component) admissionReview.getRequest().getObject();
        component.getSpec().getPre().add(entry);
        component.getSpec().getBuild().getParams().put("someKey", "some content");

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/validate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(true))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"));
    }
}

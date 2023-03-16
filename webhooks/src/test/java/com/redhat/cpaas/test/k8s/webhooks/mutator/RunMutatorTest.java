package com.redhat.cpaas.test.k8s.webhooks.mutator;

import com.redhat.agogos.k8s.webhooks.WebhookHandler;
import com.redhat.agogos.test.CRDTestServerSetup;
import com.redhat.agogos.v1alpha1.Pipeline;
import com.redhat.agogos.v1alpha1.Run;
import com.redhat.cpaas.test.k8s.webhooks.mutator.RunMutatorTest.RunMutatorTestServerSetup;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionRequest;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReviewBuilder;
import io.fabric8.kubernetes.api.model.authentication.UserInfo;
import io.fabric8.kubernetes.api.model.authentication.UserInfoBuilder;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import java.util.Base64;

@WithKubernetesTestServer(setup = RunMutatorTestServerSetup.class)
@QuarkusTest
@TestHTTPEndpoint(WebhookHandler.class)
public class RunMutatorTest {

    public static class RunMutatorTestServerSetup extends CRDTestServerSetup {
        @Override
        public void accept(KubernetesServer server) {
            super.accept(server);

            Pipeline pipeline = new Pipeline();
            pipeline.getMetadata().setName("p1");

            server.getClient().resources(Pipeline.class).inNamespace("default").resource(pipeline).create();
        }
    }

    @KubernetesTestServer
    KubernetesServer mockServer;

    Pipeline pipeline;

    Run run;

    AdmissionReview admissionReview;

    @BeforeEach
    void beforeEach() {
        run = new Run();
        run.getMetadata().setNamespace("default");
        run.getSpec().setPipeline("p1");

        UserInfo userInfo = new UserInfoBuilder().withUsername("minikube-user").withGroups("system:authenticated").build();

        AdmissionRequest admissionRequest = new AdmissionRequest();
        admissionRequest.setObject(run);
        admissionRequest.setUserInfo(userInfo);
        admissionRequest.setUid("bc1c421c-412c-40ae-86e3-52bc51b961a4");

        admissionReview = new AdmissionReviewBuilder().withRequest(admissionRequest).build();

        pipeline = mockServer.getClient().resources(Pipeline.class).inNamespace("default").withName("p1").get();
    }

    @Test
    @DisplayName("Should allow the request")
    public void testAllow() {
        JsonObject owner = Json.createObjectBuilder() //
                .add("apiVersion", "agogos.redhat.com/v1alpha1") //
                .add("kind", "Pipeline") //
                .add("name", "p1") //
                .add("uid", pipeline.getMetadata().getUid()) // This is dynamically set by the mock server
                .add("blockOwnerDeletion", true) //
                .add("controller", true) //
                .build();

        JsonObject label = Json.createObjectBuilder() //
                .add("agogos.redhat.com/pipeline", "p1") //
                .build();

        JsonArray patch = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("op", "add").add("path", "/metadata/labels").add("value", label).build())
                .add(Json.createObjectBuilder().add("op", "add").add("path", "/metadata/ownerReferences")
                        .add("value", Json.createArrayBuilder().add(owner).build())
                        .build())
                .build();

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/mutate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(true))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"))
                .body("response.patchType", CoreMatchers.equalTo("JSONPatch"))
                .body("response.patch", CoreMatchers.equalTo(Base64.getEncoder().encodeToString(patch.toString().getBytes())));
    }

    @Test
    @DisplayName("Should fail request because Pipeline does not exist")
    public void testFailMissingPipeline() {
        run.getSpec().setPipeline("doesntexist");

        RestAssured.given().when().request().contentType(ContentType.JSON).body(admissionReview).post("/mutate")
                .then().statusCode(200).body("response.allowed", CoreMatchers.equalTo(false))
                .body("response.uid", CoreMatchers.equalTo("bc1c421c-412c-40ae-86e3-52bc51b961a4"))
                .body("response.status.message", CoreMatchers.equalTo(
                        "Selected Pipeline 'doesntexist' does not exist in 'default' namespace"));
    }
}

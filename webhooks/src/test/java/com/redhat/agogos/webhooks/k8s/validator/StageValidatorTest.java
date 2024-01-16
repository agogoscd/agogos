package com.redhat.agogos.webhooks.k8s.validator;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.v1alpha1.Stage;
import com.redhat.agogos.core.v1alpha1.Stage.StageSpec;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import io.fabric8.tekton.pipeline.v1beta1.Param;
import io.fabric8.tekton.pipeline.v1beta1.ParamValue;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskRef;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

@QuarkusTest
public class StageValidatorTest {

    @InjectMock
    KubernetesFacade kubernetesFacade;

    @Inject
    StageValidator stageValidator;

    // The @Spy annotation did not work for some reason
    AdmissionResponseBuilder admissionResponseBuilder = new AdmissionResponseBuilder();
    AdmissionResponseBuilder admissionResponseBuilderSpy = Mockito.spy(admissionResponseBuilder);

    @Test
    void testValidateResourceWithEmptyStage() {
        Stage stage = new Stage();

        stageValidator.validateResource(stage, admissionResponseBuilderSpy);

        Mockito.verify(admissionResponseBuilderSpy).withAllowed(false);
    }

    @Test
    void testValidateResourceWithTaskRefName() {
        Mockito
                .when(kubernetesFacade.get(Task.class, null, "my-task"))
                .thenReturn(new Task());

        TaskRef taskRef = new TaskRef();
        taskRef.setName("my-task");
        StageSpec stageSpec = new StageSpec();
        stageSpec.setTaskRef(taskRef);
        Stage stage = new Stage();
        stage.setSpec(stageSpec);

        stageValidator.validateResource(stage, admissionResponseBuilderSpy);

        Mockito.verify(admissionResponseBuilderSpy).withAllowed(true);
    }

    @Test
    void testValidateResourceWithTaskRefResolver() {
        TaskRef taskRef = new TaskRef();
        taskRef.setResolver("cluster");
        taskRef.setParams(List.of(
                new Param("kind", new ParamValue("task")),
                new Param("namespace", new ParamValue("my-namespace")),
                new Param("name", new ParamValue("my-task"))));
        StageSpec stageSpec = new StageSpec();
        stageSpec.setTaskRef(taskRef);
        Stage stage = new Stage();
        stage.setSpec(stageSpec);

        stageValidator.validateResource(stage, admissionResponseBuilderSpy);

        Mockito.verify(admissionResponseBuilderSpy).withAllowed(true);
    }
}

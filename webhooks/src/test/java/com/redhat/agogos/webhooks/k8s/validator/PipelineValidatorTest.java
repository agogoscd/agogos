package com.redhat.agogos.webhooks.k8s.validator;

import static org.mockito.Mockito.when;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.Stage;
import com.redhat.agogos.core.v1alpha1.StageEntry;
import com.redhat.agogos.core.v1alpha1.StageEntry.StageReference;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class PipelineValidatorTest {

    @InjectMock
    KubernetesFacade kubernetesFacadeMock;

    @Inject
    PipelineValidator pipelineValidator;

    // The @Spy annotation did not work for some reason
    AdmissionResponseBuilder admissionResponseBuilder = new AdmissionResponseBuilder();
    AdmissionResponseBuilder admissionResponseBuilderSpy = Mockito.spy(admissionResponseBuilder);

    @Test
    public void validateStageRefWithNamespace() {
        when(kubernetesFacadeMock.get(Stage.class, "my-namespace", "my-stage"))
                .thenReturn(new Stage());

        Pipeline pipeline = new Pipeline();
        pipeline.getMetadata().setName(("my-pipeline"));
        pipeline.getMetadata().setNamespace("my-namespace");
        StageEntry entry = new StageEntry();
        StageReference ref = new StageReference();
        ref.setName("my-stage");
        ref.setNamespace("my-namespace");
        entry.setStageRef(ref);
        pipeline.getSpec().getStages().add(entry);

        pipelineValidator.validateResource(pipeline, admissionResponseBuilderSpy);

        Mockito.verify(admissionResponseBuilderSpy).withAllowed(true);
    }

    @Test
    public void validateStageRefWithDefaultNamespace() {
        when(kubernetesFacadeMock.get(Stage.class, "my-namespace", "my-stage"))
                .thenReturn(null);
        when(kubernetesFacadeMock.get(Stage.class, "agogos", "my-stage"))
                .thenReturn(new Stage());

        Pipeline pipeline = new Pipeline();
        pipeline.getMetadata().setName(("my-pipeline"));
        pipeline.getMetadata().setNamespace("my-namespace");
        StageEntry entry = new StageEntry();
        StageReference ref = new StageReference();
        ref.setName("my-stage");
        entry.setStageRef(ref);
        pipeline.getSpec().getStages().add(entry);

        pipelineValidator.validateResource(pipeline, admissionResponseBuilderSpy);

        Mockito.verify(admissionResponseBuilderSpy).withAllowed(true);
    }

    @Test
    public void validateStageRefNotFound() {
        when(kubernetesFacadeMock.get(Stage.class, "my-namespace", "my-stage"))
                .thenReturn(null);
        when(kubernetesFacadeMock.get(Stage.class, "agogos", "my-stage"))
                .thenReturn(new Stage());

        Pipeline pipeline = new Pipeline();
        pipeline.getMetadata().setName(("my-pipeline"));
        pipeline.getMetadata().setNamespace("my-namespace");
        StageEntry entry = new StageEntry();
        StageReference ref = new StageReference();
        ref.setName("your-stage");
        entry.setStageRef(ref);
        pipeline.getSpec().getStages().add(entry);

        pipelineValidator.validateResource(pipeline, admissionResponseBuilderSpy);

        Mockito.verify(admissionResponseBuilderSpy).withAllowed(false);
    }
}

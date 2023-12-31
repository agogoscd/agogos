package com.redhat.agogos.operator.k8s.controllers.component;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.v1alpha1.Builder;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.ComponentBuilderSpec.BuilderRef;
import io.fabric8.tekton.pipeline.v1beta1.PipelineTask;
import io.fabric8.tekton.pipeline.v1beta1.WorkspacePipelineTaskBinding;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class PipelineDependentResourceTest {

    @InjectMock
    KubernetesFacade kubernetesFacadeMock;

    @Inject
    PipelineDependentResource pipelineDependentResource;

    @Test
    public void prepareBuilderTask_adds_params() {
        Mockito
                .when(kubernetesFacadeMock.get(Builder.class, "agogos", "builder"))
                .thenReturn(new Builder("builder"));

        Component component = new Component();
        component.getMetadata().setNamespace("default");
        component.getMetadata().setName("component");
        component.getSpec().getBuild().setBuilderRef(new BuilderRef("builder"));

        PipelineTask task = pipelineDependentResource.createBuilderTask(component, new WorkspacePipelineTaskBinding());
        Assertions.assertNotNull(task);

        Assertions.assertEquals(2, task.getParams().size());
        Assertions.assertEquals(
                "$(params.params)",
                task.getParams().get(0).getValue().getStringVal());
        Assertions.assertEquals(
                "$(params.component)",
                task.getParams().get(1).getValue().getStringVal());
    }
}

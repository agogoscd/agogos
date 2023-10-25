package com.redhat.agogos.operator.k8s.controllers.build;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.ComponentBuilderSpec;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

@QuarkusTest
public class PipelineRunDependentResourceTest {

    @InjectMock
    KubernetesFacade kubernetesFacadeMock;

    @Inject
    PipelineRunDependentResource pipelineDependentResource;

    @Test
    public void createPipelineRun_adds_params() {
        ComponentBuilderSpec componentBuilderSpec = new ComponentBuilderSpec();
        componentBuilderSpec.setParams(Map.of("key", "value"));

        Component component = new Component("component");
        component.getMetadata().setNamespace("default");
        component.getSpec().setBuild(componentBuilderSpec);

        Build build = new Build();
        build.getMetadata().setName("build");
        build.getMetadata().setNamespace("default");
        build.getSpec().setComponent("component");

        PipelineRun pipelineRun = new PipelineRun();

        Mockito
                .when(kubernetesFacadeMock.get(Component.class, "default", "component"))
                .thenReturn(component);

        pipelineRun = pipelineDependentResource.createPipelineRun(build, component, pipelineRun);

        Assertions.assertEquals(
                "{\"key\":\"value\"}",
                pipelineRun.getSpec().getParams().get(1).getValue().getStringVal());
    }
}

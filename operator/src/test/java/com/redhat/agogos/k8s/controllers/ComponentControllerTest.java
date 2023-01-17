package com.redhat.agogos.k8s.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.test.CRDTestServerSetup;
import com.redhat.agogos.v1alpha1.Builder;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.ComponentBuilderSpec.BuilderRef;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.ClusterTask;
import io.fabric8.tekton.pipeline.v1beta1.ClusterTaskBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ParamSpecBuilder;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;

@WithKubernetesTestServer(setup = CRDTestServerSetup.class)
@QuarkusTest
public class ComponentControllerTest {

    @Inject
    ComponentController componentController;

    @Inject
    AgogosClient agogosClient;

    @Inject
    TektonClient tektonClient;

    @BeforeEach
    void registerInitTask() {
        ClusterTask task = new ClusterTaskBuilder().withNewMetadata().withName("init").endMetadata().build();

        tektonClient.v1beta1().clusterTasks().createOrReplace(task);
    }

    @Test
    @DisplayName("Should fail handling a Component because the Builder does not exist")
    void shouldFailBecauseOfMissingBuilder() {

        Component component = new Component();
        component.getMetadata().setNamespace("default");
        component.getMetadata().setName("test-fail");
        component.getSpec().getBuild().setBuilderRef(new BuilderRef("some-builder-name"));

        Context context = Mockito.mock(Context.class);

        UpdateControl<Component> control = componentController.reconcile(component, context);

        assertFalse(control.isUpdateResource());
        assertFalse(control.isUpdateResourceAndStatus());
        assertTrue(control.isUpdateStatus());

        assertEquals("Failed", control.getResource().getStatus().getStatus());
        assertEquals("Could not create Component: Selected Builder 'some-builder-name' is not available in the system",
                control.getResource().getStatus().getReason());
    }

    @Test
    @DisplayName("Should handle a Component")
    void shouldHandleComponent() {
        Component component = new Component();
        component.getMetadata().setNamespace("default");
        component.getMetadata().setName("test-create");
        component.getSpec().getBuild().setBuilderRef(new BuilderRef("should-handle-component-builder-name"));

        Context context = Mockito.mock(Context.class);

        Task task = new TaskBuilder().withNewMetadata().withName("should-handle-component").endMetadata().withNewSpec()
                .withParams(new ParamSpecBuilder().withName("param").build()).endSpec().build();
        tektonClient.v1beta1().tasks().inNamespace("default").createOrReplace(task);

        Builder builder = new Builder();
        builder.getMetadata().setName("should-handle-component-builder-name");
        builder.getSpec().getTaskRef().setName("should-handle-component");

        // Create the builder, we will need it later
        agogosClient.v1alpha1().builders().create(builder);

        UpdateControl<Component> control = componentController.reconcile(component, context);

        assertFalse(control.isUpdateResource());
        assertFalse(control.isUpdateResourceAndStatus());
        assertTrue(control.isUpdateStatus());

        assertEquals("Component is ready", control.getResource().getStatus().getReason());
        assertEquals("Ready", control.getResource().getStatus().getStatus());

    }
}

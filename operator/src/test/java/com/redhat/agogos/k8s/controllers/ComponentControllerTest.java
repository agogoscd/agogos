package com.redhat.agogos.k8s.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.redhat.agogos.Retries;
import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.k8s.controllers.component.ComponentController;
import com.redhat.agogos.test.CRDTestServerSetup;
import com.redhat.agogos.v1alpha1.Builder;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.ComponentBuilderSpec.BuilderRef;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.ParamSpecBuilder;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

@Disabled("Failing")
@WithKubernetesTestServer(setup = CRDTestServerSetup.class)
@QuarkusTest
public class ComponentControllerTest {

    @Inject
    ComponentController componentController;

    @Inject
    AgogosClient agogosClient;

    @Inject
    Retries retries;

    @Inject
    TektonClient tektonClient;

    @Test
    @DisplayName("Should fail handling a Component because the Builder does not exist")
    void shouldFailBecauseOfMissingBuilder() {

        Component component = new Component();
        component.getMetadata().setNamespace("default");
        component.getMetadata().setName("test-fail");
        component.getSpec().getBuild().setBuilderRef(new BuilderRef("some-builder-name"));

        @SuppressWarnings("unchecked")
        Context<Component> context = (Context<Component>) Mockito.mock(Context.class);
        when(context.getSecondaryResource(Pipeline.class)).thenReturn(Optional.empty());

        UpdateControl<Component> control = componentController.reconcile(component, context);

        assertFalse(control.isUpdateResource());
        assertFalse(control.isUpdateResourceAndStatus());
        assertTrue(control.isUpdateStatus());

        assertEquals("Failed", control.getResource().getStatus().getStatus());
        assertEquals("Could not create Component", control.getResource().getStatus().getReason());
    }

    @Test
    @DisplayName("Should handle a Component")
    void shouldHandleComponent() {
        Component component = new Component();
        component.getMetadata().setNamespace("default");
        component.getMetadata().setName("test-create");
        component.getSpec().getBuild().setBuilderRef(new BuilderRef("should-handle-component-builder-name"));

        @SuppressWarnings("unchecked")
        Context<Component> context = (Context<Component>) Mockito.mock(Context.class);
        // when(context.getSecondaryResource(Pipeline.class)).thenReturn(Optional.of(new Pipeline()));
        when(context.getSecondaryResource(Pipeline.class)).thenReturn(Optional.of(new Pipeline()));

        Task task = new TaskBuilder().withNewMetadata().withName("should-handle-component").endMetadata().withNewSpec()
                .withParams(new ParamSpecBuilder().withName("param").build()).endSpec().build();
        retries.serverSideApply(tektonClient.v1beta1().tasks().inNamespace("default").resource(task));

        Builder builder = new Builder();
        builder.getMetadata().setName("should-handle-component-builder-name");
        builder.getSpec().getTaskRef().setName("should-handle-component");

        // Create the builder, we will need it later
        agogosClient.v1alpha1().builders().resource(builder).create();

        UpdateControl<Component> control = componentController.reconcile(component, context);

        assertFalse(control.isUpdateResource());
        assertFalse(control.isUpdateResourceAndStatus());
        assertTrue(control.isUpdateStatus());

        assertEquals("Component is ready", control.getResource().getStatus().getReason());
        assertEquals("Ready", control.getResource().getStatus().getStatus());

    }
}

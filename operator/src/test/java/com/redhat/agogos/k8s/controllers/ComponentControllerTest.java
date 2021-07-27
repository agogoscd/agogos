package com.redhat.agogos.k8s.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.test.CRDTestServerSetup;
import com.redhat.agogos.v1alpha1.Builder;
import com.redhat.agogos.v1alpha1.Component;
import com.redhat.agogos.v1alpha1.Component.BuilderRef;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.ClusterTask;
import io.fabric8.tekton.pipeline.v1beta1.ClusterTaskBuilder;
import io.fabric8.tekton.pipeline.v1beta1.ParamSpecBuilder;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.fabric8.tekton.pipeline.v1beta1.TaskBuilder;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventList;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;

import java.util.List;

@WithKubernetesTestServer(setup = CRDTestServerSetup.class)
@QuarkusTest
public class ComponentControllerTest {

    @Inject
    ComponentController componentController;

    @Inject
    AgogosClient agogosClient;

    @Inject
    TektonClient tektonClient;

    private Context<Component> withCustomResourceModifiedEvent(Component component) {
        Context<Component> context = Mockito.mock(Context.class);

        CustomResourceEvent event = new CustomResourceEvent(Watcher.Action.MODIFIED, component, null);
        Mockito.when(context.getEvents()).thenReturn(new EventList(List.of(event)));

        return context;
    }

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
        component.getSpec().setBuilderRef(new BuilderRef("some-builder-name"));

        Context<Component> context = withCustomResourceModifiedEvent(component);

        UpdateControl<Component> control = componentController.createOrUpdateResource(component, context);

        assertFalse(control.isUpdateCustomResource());
        assertFalse(control.isUpdateCustomResourceAndStatusSubResource());
        assertTrue(control.isUpdateStatusSubResource());

        assertEquals("Failed", control.getCustomResource().getStatus().getStatus());
        assertEquals("Could not create Component: Selected Builder 'some-builder-name' is not available in the system",
                control.getCustomResource().getStatus().getReason());
    }

    @Test
    @DisplayName("Should handle a Component")
    void shouldHandleComponent() {
        Component component = new Component();
        component.getMetadata().setNamespace("default");
        component.getMetadata().setName("test-create");
        component.getSpec().setBuilderRef(new BuilderRef("should-handle-component-builder-name"));

        Context<Component> context = withCustomResourceModifiedEvent(component);

        Task task = new TaskBuilder().withNewMetadata().withName("should-handle-component").endMetadata().withNewSpec()
                .withParams(new ParamSpecBuilder().withName("param").build()).endSpec().build();
        tektonClient.v1beta1().tasks().inNamespace("default").createOrReplace(task);

        Builder builder = new Builder();
        builder.getMetadata().setName("should-handle-component-builder-name");
        builder.getSpec().getTaskRef().setName("should-handle-component");

        // Create the builder, we will need it later
        agogosClient.v1alpha1().builders().create(builder);

        UpdateControl<Component> control = componentController.createOrUpdateResource(component, context);

        assertFalse(control.isUpdateCustomResource());
        assertFalse(control.isUpdateCustomResourceAndStatusSubResource());
        assertTrue(control.isUpdateStatusSubResource());

        assertEquals("Component is ready", control.getCustomResource().getStatus().getReason());
        assertEquals("Ready", control.getCustomResource().getStatus().getStatus());

    }
}

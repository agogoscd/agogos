package com.redhat.agogos.k8s.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.redhat.agogos.k8s.client.AgogosClient;
import com.redhat.agogos.test.CRDTestServerSetup;
import com.redhat.agogos.v1alpha1.Builder;
import com.redhat.agogos.v1alpha1.Component;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.EventList;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;

import java.util.List;
import java.util.Map;

@WithKubernetesTestServer(setup = CRDTestServerSetup.class)
@QuarkusTest
public class ComponentControllerTest {

    @Inject
    ComponentController componentController;

    @Inject
    AgogosClient agogosClient;

    private Context<Component> withCustomResourceModifiedEvent(Component component) {
        Context<Component> context = Mockito.mock(Context.class);

        CustomResourceEvent event = new CustomResourceEvent(Watcher.Action.MODIFIED, component, null);
        Mockito.when(context.getEvents()).thenReturn(new EventList(List.of(event)));

        return context;
    }

    @Test
    @DisplayName("Should fail handling a Component because the Builder does not exist")
    void shouldFailBecauseOfMissingBuilder() {
        Component component = new Component();
        component.getMetadata().setNamespace("default");
        component.getMetadata().setName("test-fail");
        component.getSpec().setBuilderRef(Map.of("name", "some-builder-name"));

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
        component.getSpec().setBuilderRef(Map.of("name", "builder-name"));

        Context<Component> context = withCustomResourceModifiedEvent(component);

        Builder builder = new Builder();
        builder.getMetadata().setName("builder-name");

        // Create the builder, we will need it later
        agogosClient.v1alpha1().builders().create(builder);

        UpdateControl<Component> control = componentController.createOrUpdateResource(component, context);

        assertFalse(control.isUpdateCustomResource());
        assertFalse(control.isUpdateCustomResourceAndStatusSubResource());
        assertTrue(control.isUpdateStatusSubResource());

        assertEquals("Ready", control.getCustomResource().getStatus().getStatus());
        assertEquals("Component is ready", control.getCustomResource().getStatus().getReason());
    }
}

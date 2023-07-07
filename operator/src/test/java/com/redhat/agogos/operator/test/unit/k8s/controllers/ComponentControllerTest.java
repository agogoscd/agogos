package com.redhat.agogos.operator.test.unit.k8s.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.operator.k8s.controllers.component.ComponentController;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ComponentControllerTest {

    ComponentController componentController = new ComponentController();

    @Test
    @DisplayName("Default delete handler should be used")
    public void defaultDeleteShouldBeReturned() {
        DeleteControl deleteControl = componentController.cleanup(new Component(), null);

        assertEquals(deleteControl.isRemoveFinalizer(), true);
    }

}

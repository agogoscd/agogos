package com.redhat.agogos.test.unit.k8s.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.redhat.agogos.k8s.controllers.ComponentController;
import com.redhat.agogos.v1alpha1.Component;
import io.javaoperatorsdk.operator.api.DeleteControl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ComponentControllerTest {

    ComponentController componentController = new ComponentController();

    @Test
    @DisplayName("Default delete handler should be used")
    public void defaultDeleteShouldBeReturned() {
        assertEquals(componentController.deleteResource(new Component(), null), DeleteControl.DEFAULT_DELETE);
    }

}

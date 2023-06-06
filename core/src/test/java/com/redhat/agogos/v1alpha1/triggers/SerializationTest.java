package com.redhat.agogos.v1alpha1.triggers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.redhat.agogos.test.TestResources;
import com.redhat.agogos.v1alpha1.Component;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.jupiter.api.Test;

class SerializationTest {
    @Test
    void serializeComponentTest() throws java.io.IOException {
        String resource = TestResources.asString("component.yml");
        Component c = Serialization.unmarshal(resource, Component.class);
        assertEquals(resource, Serialization.asYaml(c));
    }

    @Test
    void serializeTriggerTest() throws java.io.IOException {
        String resource = TestResources.asString("trigger.yml");
        Trigger t = Serialization.unmarshal(resource, Trigger.class);
        assertEquals(resource, Serialization.asYaml(t));
    }
}
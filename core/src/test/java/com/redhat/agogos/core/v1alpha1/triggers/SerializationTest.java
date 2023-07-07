package com.redhat.agogos.core.v1alpha1.triggers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.redhat.agogos.core.test.TestResources;
import com.redhat.agogos.core.v1alpha1.Component;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SerializationTest {

    @Inject
    KubernetesSerialization mapper;

    @Test
    void serializeComponentTest() throws java.io.IOException {
        String resource = TestResources.asString("component.yml");
        Component c = mapper.unmarshal(resource, Component.class);
        assertEquals(resource, mapper.asYaml(c));
    }

    @Test
    void serializeTriggerTest() throws java.io.IOException {
        String resource = TestResources.asString("trigger.yml");
        Trigger t = mapper.unmarshal(resource, Trigger.class);
        assertEquals(resource, mapper.asYaml(t));
    }
}
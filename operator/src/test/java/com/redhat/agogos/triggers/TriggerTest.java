package com.redhat.agogos.triggers;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.agogos.k8s.AgogosConfigurationServiceProvider;
import com.redhat.agogos.v1alpha1.triggers.ComponentTriggerEvent;
import com.redhat.agogos.v1alpha1.triggers.Trigger;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import java.util.List;

@QuarkusTest
class TriggerTest {
    @Inject
    AgogosConfigurationServiceProvider.AgogosConfigurationService configurationService;

    @Test
    void verifyThatSpecIsProperlySerialized() throws JsonProcessingException {
        final var mapper = configurationService.getResourceCloner();

        final Trigger trigger = new Trigger();
        final var spec = new Trigger.TriggerSpec();
        trigger.setSpec(spec);
        final var e1 = new ComponentTriggerEvent();
        e1.setName("e1");
        final var e2 = new ComponentTriggerEvent();
        e2.setName("e2");
        spec.setEvents(List.of(e1, e2));
        final var clone = mapper.clone(trigger);
        System.out.println("trigger = " + trigger);
        System.out.println("clone = " + clone);
        assertEquals(trigger, clone);
        assertEquals(trigger.getSpec().getEvents().get(0).toCel(trigger), clone.getSpec().getEvents().get(0).toCel(clone));
    }
}
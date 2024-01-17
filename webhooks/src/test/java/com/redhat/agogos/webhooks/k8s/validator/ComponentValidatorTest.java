package com.redhat.agogos.webhooks.k8s.validator;

import static org.mockito.Mockito.when;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.v1alpha1.Builder;
import com.redhat.agogos.core.v1alpha1.Component;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
public class ComponentValidatorTest {

    @InjectMock
    KubernetesFacade kubernetesFacadeMock;

    @Inject
    ComponentValidator componentValidator;

    // The @Spy annotation did not work for some reason
    AdmissionResponseBuilder admissionResponseBuilder = new AdmissionResponseBuilder();
    AdmissionResponseBuilder admissionResponseBuilderSpy = Mockito.spy(admissionResponseBuilder);

    @Test
    public void validateEmptyComponent() {
        Component component = new Component();

        componentValidator.validateResource(component, admissionResponseBuilderSpy);

        Mockito.verify(admissionResponseBuilderSpy).withAllowed(false);
    }

    @Test
    public void validateBuilderRefJustName() {
        when(kubernetesFacadeMock.get(Builder.class, "my-namespace", "my-builder"))
                .thenReturn(null);

        when(kubernetesFacadeMock.get(Builder.class, "agogos", "my-builder"))
                .thenReturn(null);

        Component component = new Component("my-component");
        component.getMetadata().setNamespace("my-namespace");
        component.getSpec().getBuild().getBuilderRef().setName("my-builder");

        componentValidator.validateResource(component, admissionResponseBuilderSpy);

        Mockito.verify(admissionResponseBuilderSpy).withAllowed(false);
    }

    @Test
    public void validateBuilderRefWithNamespace() {
        when(kubernetesFacadeMock.get(Builder.class, "my-namespace", "my-builder"))
                .thenReturn(new Builder());

        Component component = new Component("my-component");
        component.getMetadata().setNamespace("my-namespace");
        component.getSpec().getBuild().getBuilderRef().setName("my-builder");
        component.getSpec().getBuild().getBuilderRef().setNamespace("my-namespace");

        componentValidator.validateResource(component, admissionResponseBuilderSpy);

        Mockito.verify(admissionResponseBuilderSpy).withAllowed(true);
    }

    @Test
    public void validateBuilderFromComponentNamespace() {
        when(kubernetesFacadeMock.get(Builder.class, "my-namespace", "my-builder"))
                .thenReturn(new Builder());

        Component component = new Component("my-component");
        component.getMetadata().setNamespace("my-namespace");
        component.getSpec().getBuild().getBuilderRef().setName("my-builder");

        componentValidator.validateResource(component, admissionResponseBuilderSpy);

        Mockito.verify(admissionResponseBuilderSpy).withAllowed(true);
    }

    @Test
    public void validateBuilderFromComponentAgogosNamespace() {
        when(kubernetesFacadeMock.get(Builder.class, "my-namespace", "my-builder"))
                .thenReturn(null);

        when(kubernetesFacadeMock.get(Builder.class, "agogos", "my-builder"))
                .thenReturn(new Builder());

        Component component = new Component("my-component");
        component.getMetadata().setNamespace("my-namespace");
        component.getSpec().getBuild().getBuilderRef().setName("my-builder");

        componentValidator.validateResource(component, admissionResponseBuilderSpy);

        Mockito.verify(admissionResponseBuilderSpy).withAllowed(true);
    }
}

package com.redhat.agogos.operator.k8s.controllers.dependency;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.k8s.Label;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.Dependency;
import com.redhat.agogos.core.v1alpha1.Dependents;
import com.redhat.agogos.core.v1alpha1.Group;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.Submission.SubmissionSpec;
import com.redhat.agogos.operator.eventing.CloudEventPublisher;
import com.redhat.agogos.operator.k8s.controllers.AbstractController;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.utils.KubernetesSerialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false)
public class DependencyController extends AbstractController<Dependency> {

    private static final Logger LOG = LoggerFactory.getLogger(DependencyController.class);

    private final static String TEKTON_TRIGGER_LABEL_PREFIX = "triggers.tekton.dev/";

    @Inject
    protected CloudEventPublisher cloudEventPublisher;

    @Inject
    KubernetesFacade kubernetesFacade;

    @Inject
    KubernetesSerialization objectMapper;

    @Override
    public UpdateControl<Dependency> reconcile(Dependency dependency, Context<Dependency> context) {
        // Include the Tekton trigger labels.
        Map<String, String> labels = dependency.getMetadata()
                .getLabels()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(TEKTON_TRIGGER_LABEL_PREFIX))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        labels.put(Label.NAME.toString(), dependency.getSpec().getName());
        labels.put(Label.RESOURCE.toString(), dependency.getSpec().getResource().toString().toLowerCase());
        labels.put(Label.INSTANCE.toString(), dependency.getSpec().getInstance());
        Dependents dependents = null;
        switch (dependency.getSpec().getResource()) {
            case COMPONENT:
                Component component = kubernetesFacade.get(Component.class, dependency.getMetadata().getNamespace(),
                        dependency.getSpec().getName());
                dependents = component.getSpec().getDependents();
                break;
            case GROUP:
                Group group = kubernetesFacade.get(Group.class, dependency.getMetadata().getNamespace(),
                        dependency.getSpec().getName());
                dependents = group.getSpec().getDependents();
                break;
            case PIPELINE:
                Pipeline pipeline = kubernetesFacade.get(Pipeline.class, dependency.getMetadata().getNamespace(),
                        dependency.getSpec().getName());
                dependents = pipeline.getSpec().getDependents();
                break;
            default:
                LOG.error("Unrecognized Dependency resource: " + dependency.getSpec().getResource());
                break;
        }

        if (dependents != null) {
            dependents.getComponents().stream().forEach(component -> {
                sendExecuteCloudEvent(dependency, "build", component, Component.class);
            });
            dependents.getGroups().stream().forEach(group -> {
                sendExecuteCloudEvent(dependency, "execution", group, Group.class);
            });
            dependents.getPipelines().stream().forEach(pipeline -> {
                sendExecuteCloudEvent(dependency, "run", pipeline, Pipeline.class);
            });
        }

        kubernetesFacade.delete(dependency);
        return UpdateControl.noUpdate();
    }

    private void sendExecuteCloudEvent(Dependency dependency, String state, String name, Class<? extends HasMetadata> clazz) {

        SubmissionSpec spec = new SubmissionSpec();
        spec.setName(name);
        spec.setInstance(dependency.getSpec().getInstance());
        spec.setResource(Resource.fromType(HasMetadata.getKind(clazz)));

        cloudEventPublisher.publish(dependency.getMetadata().getNamespace(), state, clazz, spec);
    }
}

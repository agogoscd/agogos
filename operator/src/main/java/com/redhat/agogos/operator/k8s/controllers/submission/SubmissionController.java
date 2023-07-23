package com.redhat.agogos.operator.k8s.controllers.submission;

import com.redhat.agogos.core.KubernetesFacade;
import com.redhat.agogos.core.k8s.Label;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.Execution;
import com.redhat.agogos.core.v1alpha1.Execution.ExecutionInfo;
import com.redhat.agogos.core.v1alpha1.Group;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.Run;
import com.redhat.agogos.core.v1alpha1.Submission;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false)
public class SubmissionController extends AbstractController<Submission> {

    private static final Logger LOG = LoggerFactory.getLogger(SubmissionController.class);

    private final static String TEKTON_TRIGGER_LABEL_PREFIX = "triggers.tekton.dev/";

    @Inject
    protected CloudEventPublisher cloudEventPublisher;

    @Inject
    KubernetesFacade kubernetesFacade;

    @Inject
    KubernetesSerialization objectMapper;

    @Override
    public UpdateControl<Submission> reconcile(Submission submission, Context<Submission> context) {
        LOG.info("Submission '{}' for '{}' of resource type {}", submission.getFullName(),
                submission.getSpec().getName(), submission.getSpec().getResource());
        // Include the Tekton trigger labels.
        Map<String, String> labels = submission.getMetadata()
                .getLabels()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(TEKTON_TRIGGER_LABEL_PREFIX))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        labels.put(Label.NAME.toString(), submission.getSpec().getName());
        labels.put(Label.RESOURCE.toString(), submission.getSpec().getResource().toString().toLowerCase());
        labels.put(Label.INSTANCE.toString(), submission.getSpec().getInstance());
        if (submission.getSpec().getGroup() != null && !submission.getSpec().getGroup().isEmpty()) {
            labels.put(Label.create(Resource.GROUP), submission.getSpec().getGroup());
        }
        switch (submission.getSpec().getResource()) {
            case COMPONENT:
                submitBuild(submission, labels);
                break;
            case GROUP:
                submitExecution(submission, labels);
                break;
            case PIPELINE:
                submitPipeline(submission, labels);
                break;
            default:
                LOG.error("Unrecognized Submission resource: " + submission.getSpec().getResource());
                break;
        }

        kubernetesFacade.delete(submission);
        return UpdateControl.noUpdate();
    }

    private void submitBuild(Submission submission, Map<String, String> labels) {
        Build build = new Build();
        build.getMetadata().setGenerateName(submission.getSpec().getName() + "-");
        build.getMetadata().setNamespace(submission.getMetadata().getNamespace());
        build.getSpec().setComponent(submission.getSpec().getName());
        build.getMetadata().setLabels(labels);
        build = kubernetesFacade.create(build);
        LOG.debug("Trigger '{}' fired and created Build '{}'",
                submission.getMetadata().getLabels().get(TEKTON_TRIGGER_LABEL_PREFIX + "trigger"),
                build.getMetadata().getName());
    }

    private void submitPipeline(Submission submission, Map<String, String> labels) {
        Run run = new Run();
        run.getMetadata().setGenerateName(submission.getSpec().getName() + "-");
        run.getMetadata().setNamespace(submission.getMetadata().getNamespace());
        run.getSpec().setPipeline(submission.getSpec().getName());
        run.getMetadata().setLabels(labels);
        run = kubernetesFacade.create(run);
        LOG.debug("Trigger '{}' fired and created Run '{}'",
                submission.getMetadata().getLabels().get(TEKTON_TRIGGER_LABEL_PREFIX + "trigger"),
                run.getMetadata().getName());
    }

    private void submitExecution(Submission submission, Map<String, String> labels) {
        Group group = kubernetesFacade.get(Group.class, submission.getMetadata().getNamespace(),
                submission.getSpec().getName());

        // Remove group label.
        labels.remove(Label.create(Resource.GROUP));

        Execution execution = new Execution();
        execution.getMetadata().setLabels(labels);
        execution.getMetadata().setGenerateName(submission.getSpec().getName() + "-");
        execution.getSpec().setGroup(submission.getSpec().getName());
        addExecutionInfo(group.getSpec().getComponents(), execution.getSpec().getComponents());
        addExecutionInfo(group.getSpec().getGroups(), execution.getSpec().getGroups());
        addExecutionInfo(group.getSpec().getPipelines(), execution.getSpec().getPipelines());
        kubernetesFacade.create(execution);

        group.getSpec().getComponents().stream().forEach(component -> {
            sendSubmissionCloudEvent(submission, "build", component, Component.class, submission.getSpec().getName());
        });
        group.getSpec().getGroups().stream().forEach(grp -> {
            sendSubmissionCloudEvent(submission, "execution", grp, Group.class, submission.getSpec().getName());
        });
        group.getSpec().getPipelines().stream().forEach(pipeline -> {
            sendSubmissionCloudEvent(submission, "run", pipeline, Pipeline.class, submission.getSpec().getName());
        });
    }

    private void addExecutionInfo(List<String> items, HashMap<String, ExecutionInfo> info) {
        items.stream().forEach(name -> info.put(name, new ExecutionInfo()));
    }

    private void sendSubmissionCloudEvent(Submission submission, String state, String name, Class<? extends HasMetadata> clazz,
            String group) {

        SubmissionSpec spec = new SubmissionSpec();
        spec.setName(name);
        spec.setInstance(submission.getSpec().getInstance());
        spec.setResource(Resource.fromType(HasMetadata.getKind(clazz)));
        spec.setGroup(group);

        cloudEventPublisher.publish(submission.getMetadata().getNamespace(), state, clazz, spec);
    }
}

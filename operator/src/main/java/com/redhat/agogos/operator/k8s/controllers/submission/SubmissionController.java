package com.redhat.agogos.operator.k8s.controllers.submission;

import com.redhat.agogos.core.k8s.Label;
import com.redhat.agogos.core.k8s.Resource;
import com.redhat.agogos.core.v1alpha1.Build;
import com.redhat.agogos.core.v1alpha1.Component;
import com.redhat.agogos.core.v1alpha1.Execution;
import com.redhat.agogos.core.v1alpha1.Execution.ExecutionComponentInfo;
import com.redhat.agogos.core.v1alpha1.Execution.ExecutionGroupInfo;
import com.redhat.agogos.core.v1alpha1.Execution.ExecutionInfo;
import com.redhat.agogos.core.v1alpha1.Execution.ExecutionPipelineInfo;
import com.redhat.agogos.core.v1alpha1.Group;
import com.redhat.agogos.core.v1alpha1.Pipeline;
import com.redhat.agogos.core.v1alpha1.Run;
import com.redhat.agogos.core.v1alpha1.Submission;
import com.redhat.agogos.core.v1alpha1.Submission.SubmissionSpec;
import com.redhat.agogos.operator.eventing.CloudEventPublisher;
import com.redhat.agogos.operator.k8s.controllers.AbstractController;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false)
public class SubmissionController extends AbstractController<Submission> {

    private static final Logger LOG = LoggerFactory.getLogger(SubmissionController.class);

    private final static String TEKTON_TRIGGER_LABEL_PREFIX = "triggers.tekton.dev/";

    @Inject
    protected CloudEventPublisher cloudEventPublisher;

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
        // If this is part of a group, use the provided name rather than generating one.
        if (labels.containsKey(Label.create(Resource.GROUP))) {
            build.getMetadata().setName(submission.getSpec().getGeneratedName());
        } else {
            build.getMetadata().setGenerateName(submission.getSpec().getName() + "-");
        }
        build.getMetadata().setNamespace(submission.getMetadata().getNamespace());
        build.getSpec().setComponent(submission.getSpec().getName());
        build.getMetadata().setLabels(labels);
        build = kubernetesFacade.create(build);

        String tLabel = submission.getMetadata().getLabels().get(TEKTON_TRIGGER_LABEL_PREFIX + "trigger");
        LOG.debug((tLabel == null ? "CLI" : "Trigger " + tLabel + " fired and") + " created Build '{}'",
                build.getMetadata().getName());
    }

    private void submitPipeline(Submission submission, Map<String, String> labels) {
        Run run = new Run();
        // If this is part of a group, use the provided name rather than generating one.
        if (labels.containsKey(Label.create(Resource.GROUP))) {
            run.getMetadata().setName(submission.getSpec().getGeneratedName());
        } else {
            run.getMetadata().setGenerateName(submission.getSpec().getName() + "-");
        }
        run.getMetadata().setNamespace(submission.getMetadata().getNamespace());
        run.getSpec().setPipeline(submission.getSpec().getName());
        run.getMetadata().setLabels(labels);
        run = kubernetesFacade.create(run);

        String tLabel = submission.getMetadata().getLabels().get(TEKTON_TRIGGER_LABEL_PREFIX + "trigger");
        LOG.debug((tLabel == null ? "CLI" : "Trigger " + tLabel + " fired and") + " created Run '{}'",
                run.getMetadata().getName());
    }

    private void submitExecution(Submission submission, Map<String, String> labels) {
        Group group = kubernetesFacade.get(Group.class, submission.getMetadata().getNamespace(),
                submission.getSpec().getName());

        if (group == null) {
            LOG.error("Unable to locate group '{}' in namespace '{}', not submitting execution.",
                    submission.getSpec().getName(),
                    submission.getMetadata().getNamespace());
            return;
        }

        // Remove group label.
        labels.remove(Label.create(Resource.GROUP));

        Execution execution = new Execution();
        execution.getMetadata().setLabels(labels);
        execution.getMetadata().setGenerateName(submission.getSpec().getName() + "-");
        execution.getMetadata().setNamespace(submission.getMetadata().getNamespace());
        execution.getSpec().setGroup(submission.getSpec().getName());
        group.getSpec().getComponents().forEach(component -> {
            execution.getSpec().getBuilds().put(generateName(component), new ExecutionComponentInfo(component));
        });
        group.getSpec().getGroups().forEach(grp -> {
            execution.getSpec().getExecutions().put(generateName(grp), new ExecutionGroupInfo(grp));
        });
        group.getSpec().getPipelines().forEach(pipeline -> {
            execution.getSpec().getRuns().put(generateName(pipeline), new ExecutionPipelineInfo(pipeline));
        });

        kubernetesFacade.create(execution);

        execution.getSpec().getBuilds().entrySet().forEach(build -> {
            sendSubmissionCloudEvent(submission, "build", build, Component.class, submission.getSpec().getName());
        });
        execution.getSpec().getExecutions().entrySet().forEach(exec -> {
            sendSubmissionCloudEvent(submission, "execution", exec, Group.class, submission.getSpec().getName());
        });
        execution.getSpec().getRuns().entrySet().forEach(run -> {
            sendSubmissionCloudEvent(submission, "run", run, Pipeline.class, submission.getSpec().getName());
        });
    }

    public String generateName(String name) {
        final Integer ASCII_0 = 48;
        final Integer ASCII_9 = 57;
        final Integer ASCII_A = 97; // Lower-case a.
        final Integer ASCII_Z = 122; // Lower-case z.
        final Integer LENGTH = 5;
        Random random = new Random();

        String generatedString = random.ints(ASCII_0, ASCII_Z + 1)
                .filter(i -> (i >= ASCII_0 && i <= ASCII_9) || (i >= ASCII_A && i <= ASCII_Z))
                .limit(LENGTH)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return String.format("%s-%s", name, generatedString);
    }

    private <T extends ExecutionInfo> void sendSubmissionCloudEvent(Submission submission, String state,
            Map.Entry<String, T> entry,
            Class<? extends HasMetadata> clazz, String group) {
        SubmissionSpec spec = new SubmissionSpec();
        if (entry.getValue() instanceof ExecutionComponentInfo) {
            spec.setName(((ExecutionComponentInfo) entry.getValue()).getComponent());
        } else if (entry.getValue() instanceof ExecutionGroupInfo) {
            spec.setName(((ExecutionGroupInfo) entry.getValue()).getGroup());
        } else if (entry.getValue() instanceof ExecutionPipelineInfo) {
            spec.setName(((ExecutionPipelineInfo) entry.getValue()).getPipeline());
        }
        spec.setGeneratedName(entry.getKey());
        spec.setInstance(submission.getSpec().getInstance());
        spec.setResource(Resource.fromType(HasMetadata.getKind(clazz)));
        spec.setGroup(group);

        cloudEventPublisher.publish(submission.getMetadata().getNamespace(), state, clazz, spec);
    }
}

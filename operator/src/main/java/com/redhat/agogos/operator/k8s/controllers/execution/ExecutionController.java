package com.redhat.agogos.operator.k8s.controllers.execution;

import com.redhat.agogos.core.ResultableResourceStatus;
import com.redhat.agogos.core.v1alpha1.Execution;
import com.redhat.agogos.core.v1alpha1.Execution.ExecutionSpec;
import com.redhat.agogos.core.v1alpha1.Group;
import com.redhat.agogos.core.v1alpha1.ResultableStatus;
import com.redhat.agogos.operator.k8s.controllers.AbstractController;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
@ControllerConfiguration(generationAwareEventProcessing = false)
public class ExecutionController extends AbstractController<Execution> {
    private static final Logger LOG = LoggerFactory.getLogger(ExecutionController.class);

    @Override
    public UpdateControl<Execution> reconcile(Execution execution, Context<Execution> context) {

        ExecutionSpec spec = execution.getSpec();

        Group group = kubernetesFacade.get(Group.class, spec.getGroup());
        if (group == null) {
            LOG.error("Unable to find group '{}'", spec.getGroup());
            return UpdateControl.noUpdate();
        }

        if (!Set.copyOf(group.getSpec().getComponents()).equals(spec.getComponents().keySet()) ||
                !Set.copyOf(group.getSpec().getGroups()).equals(spec.getGroups().keySet()) ||
                !Set.copyOf(group.getSpec().getPipelines()).equals(spec.getPipelines().keySet())) {
            LOG.debug("Number of group elements in '{}'' does not match group '{}'", execution.getMetadata().getName(),
                    spec.getGroup());
            return UpdateControl.noUpdate();
        }

        Map<ResultableResourceStatus, Long> results = Stream
                .of(spec.getComponents().values(), spec.getGroups().values(), spec.getPipelines().values())
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(x -> x.getStatus().getStatus(), Collectors.counting()));

        Stream.of(ResultableResourceStatus.values()).forEach(value -> {
            results.putIfAbsent(value, 0L);
        });

        final ResultableStatus originalStatus = objectMapper.clone(execution.getStatus());
        ResultableStatus status = execution.getStatus();
        if (status == null) {
            status = new ResultableStatus();
        }

        if (results.get(ResultableResourceStatus.NEW) == 0 && results.get(ResultableResourceStatus.RUNNING) == 0) {
            if (results.get(ResultableResourceStatus.ABORTED) > 0) {
                status.setStatus(ResultableResourceStatus.ABORTED);
            } else if (results.get(ResultableResourceStatus.FAILED) > 0) {
                status.setStatus(ResultableResourceStatus.FAILED);
            } else {
                status.setStatus(ResultableResourceStatus.FINISHED);
            }
            if (status.getCompletionTime() == null) {
                status.setCompletionTime(ResultableStatus.getFormattedNow());
            }
        } else {
            status.setStatus(ResultableResourceStatus.RUNNING);
        }

        if (status.getStartTime() == null) {
            status.setStartTime(ResultableStatus.getFormattedNow());
        }

        if (status.equals(originalStatus)) {
            LOG.debug("No change to {} status of '{}', returning noUpdate", execution.getKind(), execution.getFullName());
            return UpdateControl.noUpdate();
        }

        // Update the last update field
        status.setLastUpdate(ResultableStatus.getFormattedNow());

        // Update now, BEFORE sending the cloud event. Otherwise we are in a race condition for any interceptors.
        UpdateControl<Execution> ctrl = UpdateControl.updateStatus(execution);

        try {
            cloudEventPublisher.publish(execution);
        } catch (Exception e) {
            LOG.warn("Could not publish {} CloudEvent for {} '{}', reason: {}", execution.getKind().toLowerCase(),
                    execution.getKind(), execution.getFullName(), e.getMessage(), e);
        }

        LOG.debug("Updating {} '{}' with status '{}'", execution.getKind(), execution.getFullName(), status);

        return ctrl;
    }

}
